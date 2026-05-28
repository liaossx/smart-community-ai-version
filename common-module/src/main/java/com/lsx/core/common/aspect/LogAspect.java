package com.lsx.core.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.constant.MqConstants;
import com.lsx.core.common.dto.SysOperLogDTO;
import com.lsx.core.common.event.OperationLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     * 
     * @param joinPoint 切点
     * @param e 异常
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            // 获取当前请求对象
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // *======== 组装日志 DTO =========*//
            SysOperLogDTO operLog = new SysOperLogDTO();
            operLog.setStatus(0);
            
            // 请求的地址
            if (request != null) {
                operLog.setOperIp(request.getRemoteAddr());
                operLog.setOperUrl(request.getRequestURI());
                operLog.setRequestMethod(request.getMethod());
            }

            if (e != null) {
                operLog.setStatus(1);
                operLog.setErrorMsg(e.getMessage());
            }

            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");

            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);

            // 设置操作时间
            operLog.setOperTime(LocalDateTime.now());
            
            // 优先尝试通过 RabbitMQ 发送异步日志
            if (rabbitTemplate != null) {
                try {
                    rabbitTemplate.convertAndSend(MqConstants.OPER_LOG_EXCHANGE, MqConstants.OPER_LOG_ROUTING_KEY, operLog);
                    return; // 发送成功则不再发布本地事件
                } catch (Exception mqEx) {
                    log.warn("通过 RabbitMQ 发送日志失败，将降级为本地事件: {}", mqEx.getMessage());
                }
            }
            
            // 降级：发布本地事件，由当前服务内部监听处理
            eventPublisher.publishEvent(new OperationLogEvent(this, operLog));
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("==前置通知异常==");
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log logAnnotation, SysOperLogDTO operLog, Object jsonResult) throws Exception {
        // 设置action动作
        operLog.setBusinessType(logAnnotation.businessType().ordinal());
        // 设置标题
        operLog.setTitle(logAnnotation.title());
        // 设置操作人类别
        operLog.setOperatorType(logAnnotation.operatorType().ordinal());
        
        // 是否需要保存request，参数和值
        if (logAnnotation.isSaveRequestData()) {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, operLog);
        }
        
        // 设置返回结果
        if (jsonResult != null) {
            try {
                String result = objectMapper.writeValueAsString(jsonResult);
                // 截取长度，防止过长
                if (result.length() > 2000) {
                    result = result.substring(0, 2000);
                }
                operLog.setJsonResult(result);
            } catch (Exception e) {
                // ignore
            }
        }

        // 获取当前操作人员
        try {
            String username = UserContext.getUsername();
            if (username != null) {
                operLog.setOperName(username);
            } else {
                operLog.setOperName("未知用户");
            }
        } catch (Exception e) {
             operLog.setOperName("系统");
        }
    }

    /**
     * 获取请求的参数，放到log中
     */
    private void setRequestValue(JoinPoint joinPoint, SysOperLogDTO operLog) {
        String params = argsArrayToString(joinPoint.getArgs());
        // 截取长度
        if (params.length() > 2000) {
            params = params.substring(0, 2000);
        }
        operLog.setOperParam(params);
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray) {
        if (paramsArray == null || paramsArray.length == 0) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(paramsArray);
        } catch (Exception e) {
            return "";
        }
    }
}
