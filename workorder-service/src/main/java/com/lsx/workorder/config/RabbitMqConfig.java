package com.lsx.workorder.config;

import com.lsx.core.common.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    /**
     * 报修交换机
     */
    @Bean
    public DirectExchange repairExchange() {
        return new DirectExchange(MqConstants.REPAIR_EXCHANGE, true, false);
    }

    /**
     * 报修提交队列
     */
    @Bean
    public Queue repairSubmitQueue() {
        return new Queue(MqConstants.REPAIR_SUBMIT_QUEUE, true);
    }

    /**
     * 绑定报修提交队列到交换机
     */
    @Bean
    public Binding repairSubmitBinding() {
        return BindingBuilder.bind(repairSubmitQueue()).to(repairExchange()).with(MqConstants.REPAIR_SUBMIT_ROUTING_KEY);
    }
}
