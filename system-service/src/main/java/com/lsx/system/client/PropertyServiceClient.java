package com.lsx.system.client;

import com.lsx.core.common.Result.Result;
import com.lsx.system.dto.NoticeCreateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.List;

@FeignClient(name = "property-service", path = "/api", fallbackFactory = com.lsx.system.client.fallback.PropertyServiceClientFallbackFactory.class)
public interface PropertyServiceClient {

    @PostMapping("/notice/inner")
    Result<Long> createNoticeInner(@RequestBody NoticeCreateDTO dto,
                                   @RequestParam("userId") Long userId,
                                   @RequestHeader(value = "X-Internal-Token", required = false) String internalToken);

    @GetMapping("/complaint/stats")
    Map<String, Object> getComplaintStats();

    @GetMapping("/complaint/stats/type")
    List<Map<String, Object>> getComplaintTypeStats();

    @GetMapping("/repair/stats")
    Map<String, Object> getRepairStats();
    
    @GetMapping("/repair/stats/trend")
    List<Map<String, Object>> getRepairTrend();

    @GetMapping("/visitor/stats")
    Map<String, Object> getVisitorStats();
}
