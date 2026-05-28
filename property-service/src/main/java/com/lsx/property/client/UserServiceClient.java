package com.lsx.property.client;

import com.lsx.property.dto.external.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", path = "/api/user")
public interface UserServiceClient {

    @GetMapping("/inner/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
    
    @GetMapping("/count")
    Long countUsers();
    
    @GetMapping("/count/role")
    Long countUsersByRole(@RequestParam("role") String role);

    @GetMapping("/{id}/realname")
    String getRealNameById(@PathVariable("id") Long id);
}
