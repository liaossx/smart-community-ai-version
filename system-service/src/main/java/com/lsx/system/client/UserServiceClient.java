package com.lsx.system.client;

import com.lsx.system.dto.external.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", path = "/api/user", fallbackFactory = com.lsx.system.client.fallback.UserServiceClientFallbackFactory.class)
public interface UserServiceClient {

    @GetMapping("/inner/{id}")
    UserInfoDTO getUserById(@PathVariable("id") Long id);

    @GetMapping("/count")
    Long countUsers();

    @GetMapping("/count/role")
    Long countUsersByRole(@RequestParam("role") String role);
}
