package com.lsx.workorder.client;

import com.lsx.core.common.Result.Result;
import com.lsx.workorder.dto.external.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    
    @GetMapping("/api/user/inner/list/role")
    Result<List<UserInfoDTO>> getUsersByRole(@RequestParam("role") String role);

    @GetMapping("/api/user/inner/{id}")
    UserInfoDTO getUserById(@PathVariable("id") Long id);
}
