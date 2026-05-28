package com.lsx.house.client.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class UserServiceFeignConfig {
    @Bean
    public RequestInterceptor userServiceInternalTokenInterceptor(
            @Value("${internal.token:}") String internalToken) {
        return template -> {
            template.header("Authorization");
            if (internalToken != null && !internalToken.isEmpty()) {
                template.header("X-Internal-Token", internalToken);
            }
        };
    }
}
