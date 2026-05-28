package com.lsx.system.config;

import com.lsx.core.common.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {

    /**
     * 操作日志交换机
     */
    @Bean
    public DirectExchange operLogExchange() {
        return new DirectExchange(MqConstants.OPER_LOG_EXCHANGE, true, false);
    }

    /**
     * 操作日志队列
     */
    @Bean
    public Queue operLogQueue() {
        return new Queue(MqConstants.OPER_LOG_QUEUE, true);
    }

    /**
     * 绑定操作日志队列到交换机
     */
    @Bean
    public Binding operLogBinding() {
        return BindingBuilder.bind(operLogQueue()).to(operLogExchange()).with(MqConstants.OPER_LOG_ROUTING_KEY);
    }
}
