package com.lsx.core.common.event;

import com.lsx.core.common.dto.SysOperLogDTO;
import org.springframework.context.ApplicationEvent;

/**
 * 操作日志事件
 */
public class OperationLogEvent extends ApplicationEvent {

    private final SysOperLogDTO operLog;

    public OperationLogEvent(Object source, SysOperLogDTO operLog) {
        super(source);
        this.operLog = operLog;
    }

    public SysOperLogDTO getOperLog() {
        return operLog;
    }
}
