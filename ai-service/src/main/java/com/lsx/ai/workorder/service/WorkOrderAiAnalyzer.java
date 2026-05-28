package com.lsx.ai.workorder.service;

import com.lsx.ai.workorder.dto.WorkOrderAnalyzeRequest;
import com.lsx.ai.workorder.dto.WorkOrderAnalyzeResponse;

public interface WorkOrderAiAnalyzer {
    WorkOrderAnalyzeResponse analyze(WorkOrderAnalyzeRequest request);
}
