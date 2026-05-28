package com.lsx.property.client;

import com.lsx.core.common.Result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "system-service", contextId = "systemConfigClient", path = "/api/system/config", configuration = com.lsx.property.client.config.SystemConfigFeignConfig.class)
public interface SystemConfigClient {
    @GetMapping("/inner/list")
    Result<String> getValue(@RequestParam("configKey") String configKey);
}
