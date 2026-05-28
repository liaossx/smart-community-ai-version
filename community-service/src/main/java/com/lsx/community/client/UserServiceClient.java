package com.lsx.community.client;

import com.lsx.community.dto.external.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/api/user")
public interface UserServiceClient {

    @GetMapping("/inner/{id}")
    UserInfoDTO getUserById(@PathVariable("id") Long id);
}
