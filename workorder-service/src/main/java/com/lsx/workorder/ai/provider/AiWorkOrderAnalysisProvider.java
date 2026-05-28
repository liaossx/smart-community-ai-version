package com.lsx.workorder.ai.provider;

import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.repair.entity.Repair;

public interface AiWorkOrderAnalysisProvider {
    AiWorkOrderAnalyzeResult analyze(Repair repair);
}
