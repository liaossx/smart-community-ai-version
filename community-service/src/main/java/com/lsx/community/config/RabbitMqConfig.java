package com.lsx.community.config;

import com.lsx.core.common.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RabbitMqConfig {

    @PostConstruct
    public void init() {
        log.info("初始化 RabbitMQ 配置...");
    }

    @Bean
    public DirectExchange activityExchange() {
        log.info("创建交换机: {}", MqConstants.ACTIVITY_EXCHANGE);
        return new DirectExchange(MqConstants.ACTIVITY_EXCHANGE);
    }

    @Bean
    public Queue activitySignupQueue() {
        log.info("创建队列: {}", MqConstants.ACTIVITY_SIGNUP_QUEUE);
        return new Queue(MqConstants.ACTIVITY_SIGNUP_QUEUE, true);
    }

    @Bean
    public Binding activitySignupBinding(Queue activitySignupQueue, DirectExchange activityExchange) {
        log.info("绑定队列 {} 到交换机 {} 路由键 {}", MqConstants.ACTIVITY_SIGNUP_QUEUE, MqConstants.ACTIVITY_EXCHANGE, MqConstants.ACTIVITY_SIGNUP_ROUTING_KEY);
        return BindingBuilder.bind(activitySignupQueue).to(activityExchange).with(MqConstants.ACTIVITY_SIGNUP_ROUTING_KEY);
    }
}