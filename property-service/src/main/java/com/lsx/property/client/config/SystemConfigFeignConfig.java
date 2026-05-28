package com.lsx.property.client.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class SystemConfigFeignConfig {
    @Bean
    public RequestInterceptor systemConfigInternalTokenInterceptor(
            @Value("${internal.token:}") String internalToken) {
        return template -> {
            template.header("Authorization");
            if (internalToken != null && !internalToken.isEmpty()) {
                template.header("X-Internal-Token", internalToken);
            }
        };
    }
}
