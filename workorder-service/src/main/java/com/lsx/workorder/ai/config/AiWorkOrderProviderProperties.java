package com.lsx.workorder.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-community.ai.workorder")
public class AiWorkOrderProviderProperties {
    private String provider = "rule";
    private String aiServiceUrl = "http://localhost:8090";
}
