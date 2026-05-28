package com.lsx.workorder.ai.service;

import com.lsx.workorder.ai.entity.AiWorkOrderAnalysis;

import java.util.List;

public interface AiWorkOrderAnalysisService {
    AiWorkOrderAnalysis analyzeRepair(Long repairId);

    AiWorkOrderAnalysis getLatestAnalysis(Long repairId);

    List<AiWorkOrderAnalysis> getAnalysisHistory(Long repairId);
}
