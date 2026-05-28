package com.lsx.system.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "workorder-service", path = "/api", fallbackFactory = com.lsx.system.client.fallback.WorkorderServiceClientFallbackFactory.class)
public interface WorkorderServiceClient {

    @GetMapping("/repair/stats")
    Map<String, Object> getRepairStats();

    @GetMapping("/repair/stats/trend")
    List<Map<String, Object>> getRepairTrend();
}
