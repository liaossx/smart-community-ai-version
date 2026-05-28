package com.lsx.workorder.ai.service;

import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeRequest;
import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.repair.entity.Repair;

public interface AiWorkOrderService {
    AiWorkOrderAnalyzeResult analyze(AiWorkOrderAnalyzeRequest request);

    AiWorkOrderAnalyzeResult analyzeRepair(Repair repair);
}
