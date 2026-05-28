package com.lsx.workorder.client;

import com.lsx.workorder.ai.dto.AiServiceWorkOrderAnalyzeRequest;
import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service", url = "${smart-community.ai.workorder.ai-service-url:http://localhost:8090}")
public interface AiServiceClient {

    @PostMapping("/api/ai/workorder/analyze")
    AiWorkOrderAnalyzeResult analyzeWorkOrder(@RequestBody AiServiceWorkOrderAnalyzeRequest request);
}
