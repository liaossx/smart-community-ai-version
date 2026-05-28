package com.lsx.workorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.lsx.workorder", "com.lsx.core.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.lsx.workorder.client", "com.lsx.core.common.client"})
@MapperScan("com.lsx.workorder.*.mapper")
public class WorkorderApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkorderApplication.class, args);
    }
}