package com.lsx.system.mq.listener;

import com.lsx.core.common.constant.MqConstants;
import com.lsx.core.common.dto.SysOperLogDTO;
import com.lsx.system.entity.SysOperLog;
import com.lsx.system.service.SysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class OperLogMsgListener {

    @Resource
    private SysOperLogService operLogService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.OPER_LOG_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstants.OPER_LOG_EXCHANGE),
            key = MqConstants.OPER_LOG_ROUTING_KEY
    ))
    public void listenOperLog(SysOperLogDTO operLogDTO) {
        log.info("收到异步操作日志消息：{} - {}", operLogDTO.getTitle(), operLogDTO.getOperName());
        
        try {
            SysOperLog operLog = new SysOperLog();
            BeanUtils.copyProperties(operLogDTO, operLog);
            operLogService.save(operLog);
            log.info("异步操作日志已保存到数据库");
        } catch (Exception e) {
            log.error("保存异步操作日志失败", e);
        }
    }
}
