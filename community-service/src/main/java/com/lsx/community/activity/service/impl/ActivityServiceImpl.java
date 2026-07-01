package com.lsx.community.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.community.activity.dto.ActivitySignupMessageDTO;
import com.lsx.community.activity.dto.SignupRecordDTO;
import com.lsx.community.activity.entity.SysActivity;
import com.lsx.community.activity.entity.SysActivitySignup;
import com.lsx.community.activity.mapper.SysActivityMapper;
import com.lsx.community.activity.mapper.SysActivitySignupMapper;
import com.lsx.community.activity.service.ActivityService;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.common.constant.MqConstants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl extends ServiceImpl<SysActivityMapper, SysActivity> implements ActivityService {

    @Autowired
    private SysActivitySignupMapper signupMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // ======== Lua 脚本：将查重 + 初始化库存 + 扣库存 + 标记用户 合并为一次原子操作 ========
    private static final String JOIN_LUA_SCRIPT = String.join("\n",
            "local stockKey = KEYS[1]",
            "local userSetKey = KEYS[2]",
            "local userId = ARGV[1]",
            "local initialStock = tonumber(ARGV[2])",
            "",
            "-- 1. 检查是否重复报名",
            "if redis.call('SISMEMBER', userSetKey, userId) == 1 then",
            "    return {0, 'DUPLICATE'}",
            "end",
            "",
            "-- 2. 库存 key 不存在则初始化（从 MySQL 算好的 initialStock 传入）",
            "if redis.call('EXISTS', stockKey) == 0 then",
            "    redis.call('SET', stockKey, initialStock)",
            "end",
            "",
            "-- 3. 原子扣库存",
            "local stock = redis.call('DECR', stockKey)",
            "if stock < 0 then",
            "    redis.call('INCR', stockKey)",
            "    return {0, 'SOLD_OUT'}",
            "end",
            "",
            "-- 4. 标记用户已报名",
            "redis.call('SADD', userSetKey, userId)",
            "",
            "return {1, 'OK'}"
    );

    private DefaultRedisScript<List> joinScript;

    @PostConstruct
    public void initJoinScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(JOIN_LUA_SCRIPT);
        script.setResultType(List.class);
        this.joinScript = script;
    }

    @Override
    public IPage<SysActivity> list(String status, Integer pageNum, Integer pageSize) {
        Page<SysActivity> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysActivity> qw = new QueryWrapper<>();
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null) qw.eq("community_id", cid);
        }
        if (status != null && status.trim().length() > 0) {
            qw.eq("status", status);
        }
        qw.orderByDesc("start_time");
        return this.page(page, qw);
    }

    @Override
    public SysActivity detail(Long id) {
        return this.getById(id);
    }

    @Override
    public Long publish(SysActivity a) {
        Long cid = UserContext.getCommunityId();
        
        if (a.getId() != null) {
            SysActivity exist = this.getById(a.getId());
            if (exist == null) {
                throw new RuntimeException("活动不存在");
            }
            
            String role = UserContext.getRole();
            if (!"super_admin".equalsIgnoreCase(role)) {
                if (cid != null && !cid.equals(exist.getCommunityId())) {
                    throw new RuntimeException("无权编辑其他社区活动");
                }
            }
            
            exist.setTitle(a.getTitle());
            exist.setContent(a.getContent());
            exist.setStartTime(a.getStartTime());
            exist.setLocation(a.getLocation());
            exist.setMaxCount(a.getMaxCount());
            if (a.getCoverUrl() != null) {
                 exist.setCoverUrl(a.getCoverUrl());
            }
            if (a.getStatus() != null) {
                exist.setStatus(a.getStatus());
            }
            
            this.updateById(exist);
            cacheActivityInfo(exist.getId(), exist.getStatus(),
                    exist.getMaxCount() == null ? 999999 : exist.getMaxCount(),
                    exist.getSignupCount() == null ? 0 : exist.getSignupCount());
            return exist.getId();
        } else {
            a.setCommunityId(cid);
            if (a.getStatus() == null || a.getStatus().trim().isEmpty()) {
                a.setStatus("PUBLISHED");
            }
            if (a.getSignupCount() == null) {
                a.setSignupCount(0);
            }
            if (a.getCreateTime() == null) {
                a.setCreateTime(LocalDateTime.now());
            }
            this.save(a);
            cacheActivityInfo(a.getId(), a.getStatus(),
                    a.getMaxCount() == null ? 999999 : a.getMaxCount(),
                    a.getSignupCount() == null ? 0 : a.getSignupCount());
            return a.getId();
        }
    }

    @Override
    public boolean deleteByIdWithCheck(Long id) {
        SysActivity a = this.getById(id);
        if (a == null) return false;
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid == null || a.getCommunityId() == null || !cid.equals(a.getCommunityId())) {
                throw new RuntimeException("无权删除其他社区活动");
            }
        }
        return this.removeById(id);
    }

    @Override
    public boolean join(Long activityId, Long userId) {
        String stockKey = "activity:stock:" + activityId;
        String userSetKey = "activity:users:" + activityId;
        String infoKey = "activity:info:" + activityId;

        // 1. 从 Redis 读活动状态和上限（publish 时已写入），避免每次查 DB
        Map<Object, Object> info = stringRedisTemplate.opsForHash().entries(infoKey);
        String status;
        int maxCount;

        if (!info.isEmpty()) {
            status = (String) info.get("status");
            maxCount = Integer.parseInt((String) info.get("maxCount"));
        } else {
            // 缓存未命中 → 回源 DB 一次，并回填缓存
            SysActivity a = this.getById(activityId);
            if (a == null) throw new RuntimeException("活动不存在");
            status = a.getStatus();
            maxCount = a.getMaxCount() == null ? 999999 : a.getMaxCount();
            cacheActivityInfo(activityId, status, maxCount,
                    a.getSignupCount() == null ? 0 : a.getSignupCount());
        }

        if (!"ONLINE".equals(status) && !"PUBLISHED".equals(status)) {
            throw new RuntimeException("活动不可报名");
        }

        // 2. initialStock 只在 stockKey 不存在时被 Lua 使用（SET 初始化）
        //    绝大多数请求下 stockKey 已存在 → 不需要查 DB
        int initialStock = maxCount;
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
            SysActivity a = this.getById(activityId);
            if (a == null) throw new RuntimeException("活动不存在");
            int currentCount = a.getSignupCount() == null ? 0 : a.getSignupCount();
            initialStock = maxCount - currentCount;
        }

        // 3. 一次 EVAL 完成查重 + 扣库存 + 标记用户
        @SuppressWarnings("unchecked")
        List<Object> result = stringRedisTemplate.execute(
                joinScript,
                java.util.Arrays.asList(stockKey, userSetKey),
                userId.toString(),
                String.valueOf(initialStock)
        );

        long code = (long) result.get(0);
        if (code == 0) {
            String reason = (String) result.get(1);
            if ("DUPLICATE".equals(reason)) {
                throw new RuntimeException("您已报名该活动");
            } else {
                throw new RuntimeException("名额已满");
            }
        }

        // 4. 异步 MQ 落库
        ActivitySignupMessageDTO msg = new ActivitySignupMessageDTO();
        msg.setActivityId(activityId);
        msg.setUserId(userId);
        rabbitTemplate.convertAndSend(
                MqConstants.ACTIVITY_EXCHANGE,
                MqConstants.ACTIVITY_SIGNUP_ROUTING_KEY,
                msg
        );

        return true;
    }

    private void cacheActivityInfo(Long activityId, String status, int maxCount, int signupCount) {
        Map<String, String> cache = new HashMap<>();
        cache.put("status", status);
        cache.put("maxCount", String.valueOf(maxCount));
        cache.put("signupCount", String.valueOf(signupCount));
        stringRedisTemplate.opsForHash().putAll("activity:info:" + activityId, cache);
    }
    
    @Override
    public IPage<SignupRecordDTO> getSignupList(Long activityId, Integer pageNum, Integer pageSize) {
        Page<SignupRecordDTO> page = new Page<>(pageNum, pageSize);
        SysActivity a = this.getById(activityId);
        if (a == null) throw new RuntimeException("活动不存在");
        
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null && !cid.equals(a.getCommunityId())) {
                 throw new RuntimeException("无权查看其他社区活动报名");
            }
        }
        
        return signupMapper.selectSignupList(page, activityId);
    }
}
