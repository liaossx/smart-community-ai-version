package com.lsx.community.activity.listener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.community.activity.dto.ActivitySignupMessageDTO;
import com.lsx.community.activity.entity.SysActivity;
import com.lsx.community.activity.entity.SysActivitySignup;
import com.lsx.community.activity.mapper.SysActivityMapper;
import com.lsx.community.activity.mapper.SysActivitySignupMapper;
import com.lsx.core.common.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ActivitySignupListener {

    @Autowired
    private SysActivitySignupMapper signupMapper;

    @Autowired
    private SysActivityMapper activityMapper;

    @RabbitListener(queues = MqConstants.ACTIVITY_SIGNUP_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleActivitySignupMessage(ActivitySignupMessageDTO msg) {
        log.info("接收到活动报名异步处理消息: activityId={}, userId={}", msg.getActivityId(), msg.getUserId());

        try {
            Long activityId = msg.getActivityId();
            Long userId = msg.getUserId();

            // 1. 插入报名记录（由唯一索引保证幂等：uk_activity_user(activity_id, user_id)）
            SysActivitySignup signup = new SysActivitySignup();
            signup.setActivityId(activityId);
            signup.setUserId(userId);
            signup.setSignupTime(LocalDateTime.now());
            signupMapper.insert(signup);

            // 2. 更新活动报名人数（使用原子 SQL，避免并发下“读-改-写”丢失更新）
            activityMapper.update(
                    null,
                    Wrappers.<SysActivity>lambdaUpdate()
                            .eq(SysActivity::getId, activityId)
                            .setSql("signup_count = IFNULL(signup_count, 0) + 1")
            );

            log.info("活动报名异步处理成功: activityId={}, userId={}", activityId, userId);
        } catch (DuplicateKeyException dup) {
            log.warn("活动报名消息重复，已存在报名记录，忽略：activityId={}, userId={}", msg.getActivityId(), msg.getUserId());
        } catch (Exception e) {
            log.error("活动报名异步处理异常: ", e);
            // 这里可以考虑将失败消息写入死信队列或死信表，人工介入补偿
            throw e; // 抛出异常让消息重试
        }
    }
}
