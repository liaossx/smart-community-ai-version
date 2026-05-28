package com.lsx.system.mq.listener;

import com.lsx.core.common.Result.Result;
import com.lsx.core.common.constant.MqConstants;
import com.lsx.core.common.dto.mq.RepairMsgDTO;
import com.lsx.system.client.PropertyServiceClient;
import com.lsx.system.dto.NoticeCreateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;

@Component
@Slf4j
public class RepairMsgListener {

    @Resource
    private PropertyServiceClient propertyServiceClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${internal.token:}")
    private String internalToken;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.REPAIR_SUBMIT_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstants.REPAIR_EXCHANGE),
            key = MqConstants.REPAIR_SUBMIT_ROUTING_KEY
    ))
    public void listenRepairSubmit(RepairMsgDTO msg) {
        log.info("收到报修异步通知：报修单ID={}, 故障类型={}, 提交人ID={}", 
                msg.getRepairId(), msg.getFaultType(), msg.getUserId());

        String idempotentKey = "mq:repair:submit:" + msg.getRepairId();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", Duration.ofDays(1));
        if (locked == null || !locked) {
            log.info("报修异步通知已处理，跳过：报修单ID={}", msg.getRepairId());
            return;
        }

        NoticeCreateDTO notice = new NoticeCreateDTO();
        notice.setTitle("报修受理通知");
        notice.setContent("您提交的报修（单号：" + msg.getRepairId() + "，类型：" + msg.getFaultType() + "）已成功提交，我们将尽快安排处理。");
        notice.setTargetType("USER");
        notice.setTargetUserId(msg.getUserId());
        notice.setCommunityId(msg.getCommunityId());
        notice.setPublishStatus("PUBLISHED");
        notice.setTopFlag(false);

        Result<Long> result = propertyServiceClient.createNoticeInner(notice, msg.getUserId(), internalToken);
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            stringRedisTemplate.delete(idempotentKey);
            throw new RuntimeException(result != null ? result.getMsg() : "发送站内信通知失败");
        }
        log.info("已发送站内信通知，通知ID: {}", result.getData());

        log.info("【模拟短信发送】To用户ID{}: 您的报修单（ID:{}）已提交成功，请留意后续进度。", msg.getUserId(), msg.getRepairId());
        log.info("报修异步通知处理完成");
    }
}
