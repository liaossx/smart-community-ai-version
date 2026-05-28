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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivityServiceImpl extends ServiceImpl<SysActivityMapper, SysActivity> implements ActivityService {

    @Autowired
    private SysActivitySignupMapper signupMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

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
        // ==========================
        // 测试第二阶段：优化后（Redis+MQ）
        // ==========================

        SysActivity a = this.getById(activityId);
        if (a == null) throw new RuntimeException("活动不存在");

        if (!"ONLINE".equals(a.getStatus()) && !"PUBLISHED".equals(a.getStatus())) {
             throw new RuntimeException("活动不可报名");
        }

        String stockKey = "activity:stock:" + activityId;
        String userSetKey = "activity:users:" + activityId;

        // 1. 如果Redis中没有库存，则从数据库加载（简单防缓存击穿）
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
            int maxCount = a.getMaxCount() == null ? 999999 : a.getMaxCount();
            int currentCount = a.getSignupCount() == null ? 0 : a.getSignupCount();
            int remain = maxCount - currentCount;
            // 使用 setIfAbsent 避免并发覆盖
            stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(remain));
        }

        // 2. 利用 Redis Set 防止重复报名
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(userSetKey, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            throw new RuntimeException("您已报名该活动");
        }

        // 3. Redis 预减库存
        Long stock = stringRedisTemplate.opsForValue().decrement(stockKey);
        if (stock != null && stock < 0) {
            // 库存不足，恢复库存
            stringRedisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("名额已满");
        }

        // 4. 将用户加入已报名集合
        stringRedisTemplate.opsForSet().add(userSetKey, userId.toString());

        // 5. 异步发送 MQ 消息，进行数据库写库
        ActivitySignupMessageDTO msg = new ActivitySignupMessageDTO();
        msg.setActivityId(activityId);
        msg.setUserId(userId);
        rabbitTemplate.convertAndSend(MqConstants.ACTIVITY_EXCHANGE, MqConstants.ACTIVITY_SIGNUP_ROUTING_KEY, msg);

        return true;
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
