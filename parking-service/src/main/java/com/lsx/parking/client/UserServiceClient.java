package com.lsx.parking.client;

import com.lsx.parking.dto.external.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/api/user")
public interface UserServiceClient {

    @GetMapping("/inner/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
