package com.lsx.workorder.ai.repository;

import com.lsx.workorder.ai.entity.AiWorkOrderAnalysis;

import java.util.List;
import java.util.Optional;

public interface AiWorkOrderAnalysisStore {
    void clearLatest(Long repairId);

    AiWorkOrderAnalysis save(AiWorkOrderAnalysis analysis);

    Optional<AiWorkOrderAnalysis> findLatestByRepairId(Long repairId);

    List<AiWorkOrderAnalysis> findHistoryByRepairId(Long repairId);
}
