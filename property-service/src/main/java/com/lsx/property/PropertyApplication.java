package com.lsx.property;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.lsx.property", "com.lsx.core.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.lsx.property.client", "com.lsx.core.common.client"})
@EnableScheduling
@MapperScan("com.lsx.property.*.mapper")
public class PropertyApplication {
    public static void main(String[] args) {
        SpringApplication.run(PropertyApplication.class, args);
    }
}
