package com.lsx.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.lsx.system", "com.lsx.core.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.lsx.system.client", "com.lsx.core.common.client"})
@MapperScan("com.lsx.system.mapper")
public class SystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
