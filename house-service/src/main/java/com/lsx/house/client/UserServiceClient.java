package com.lsx.house.client;

import com.lsx.house.dto.external.UserInfoDTO;
import com.lsx.core.common.Result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", contextId = "houseUserServiceClient", path = "/api/user", configuration = com.lsx.house.client.config.UserServiceFeignConfig.class)
public interface UserServiceClient {
    @GetMapping("/inner/{id}")
    UserInfoDTO getUserById(@PathVariable("id") Long id);

    @PutMapping("/inner/{id}/community")
    Result<Boolean> updateUserCommunityIdIfEmpty(@PathVariable("id") Long id, @RequestParam("communityId") Long communityId);
}
