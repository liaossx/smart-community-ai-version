package com.lsx.workorder.ai.provider.impl;

import com.lsx.workorder.ai.dto.AiServiceWorkOrderAnalyzeRequest;
import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.ai.provider.AiWorkOrderAnalysisProvider;
import com.lsx.workorder.client.AiServiceClient;
import com.lsx.workorder.repair.entity.Repair;
import org.springframework.stereotype.Component;

@Component
public class SpringAiWorkOrderAnalysisProvider implements AiWorkOrderAnalysisProvider {
    private final AiServiceClient aiServiceClient;

    public SpringAiWorkOrderAnalysisProvider(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @Override
    public AiWorkOrderAnalyzeResult analyze(Repair repair) {
        AiServiceWorkOrderAnalyzeRequest request = new AiServiceWorkOrderAnalyzeRequest();
        if (repair != null) {
            request.setRepairId(repair.getId());
            request.setFaultType(repair.getFaultType());
            request.setFaultDesc(repair.getFaultDesc());
        }
        return aiServiceClient.analyzeWorkOrder(request);
    }
}
