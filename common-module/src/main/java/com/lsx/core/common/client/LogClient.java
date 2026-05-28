package com.lsx.core.common.client;

import com.lsx.core.common.dto.SysOperLogDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "system-service", path = "/api/monitor/operlog")
public interface LogClient {

    @PostMapping
    void saveLog(@RequestBody SysOperLogDTO operLog);
}
