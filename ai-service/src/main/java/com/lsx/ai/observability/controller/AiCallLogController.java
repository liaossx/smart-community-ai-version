package com.lsx.ai.observability.controller;

import com.lsx.ai.observability.dto.AiCallLogPageResponse;
import com.lsx.ai.observability.service.AiCallLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/observability")
public class AiCallLogController {
    private final AiCallLogService aiCallLogService;

    public AiCallLogController(AiCallLogService aiCallLogService) {
        this.aiCallLogService = aiCallLogService;
    }

    @GetMapping("/call-logs")
    public AiCallLogPageResponse listCallLogs(
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return aiCallLogService.list(bizType, status, pageNum, pageSize);
    }
}
