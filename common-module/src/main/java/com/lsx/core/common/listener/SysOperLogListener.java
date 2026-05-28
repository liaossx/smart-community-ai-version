package com.lsx.core.common.listener;

import com.lsx.core.common.client.LogClient;
import com.lsx.core.common.dto.SysOperLogDTO;
import com.lsx.core.common.event.OperationLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 异步监听日志事件并调用远程日志服务保存
 */
@Component
@Slf4j
public class SysOperLogListener {

    @Resource
    private LogClient logClient;

    @Async // 异步执行，不影响业务响应速度
    @EventListener
    public void saveSysLog(OperationLogEvent event) {
        try {
            SysOperLogDTO logDto = event.getOperLog();
            logClient.saveLog(logDto);
        } catch (Exception e) {
            log.error("调用远程服务保存操作日志失败:{}", e.getMessage());
        }
    }
}
