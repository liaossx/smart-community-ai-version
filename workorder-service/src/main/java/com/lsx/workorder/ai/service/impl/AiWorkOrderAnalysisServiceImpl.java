package com.lsx.workorder.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.ai.entity.AiWorkOrderAnalysis;
import com.lsx.workorder.ai.provider.AiWorkOrderAnalysisProvider;
import com.lsx.workorder.ai.repository.AiWorkOrderAnalysisStore;
import com.lsx.workorder.ai.service.AiWorkOrderAnalysisService;
import com.lsx.workorder.repair.entity.Repair;
import com.lsx.workorder.repair.service.RepairService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiWorkOrderAnalysisServiceImpl implements AiWorkOrderAnalysisService {

    private final RepairService repairService;
    private final AiWorkOrderAnalysisProvider provider;
    private final AiWorkOrderAnalysisStore store;

    public AiWorkOrderAnalysisServiceImpl(RepairService repairService,
                                          AiWorkOrderAnalysisProvider provider,
                                          AiWorkOrderAnalysisStore store) {
        this.repairService = repairService;
        this.provider = provider;
        this.store = store;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiWorkOrderAnalysis analyzeRepair(Long repairId) {
        Repair repair = repairService.getById(repairId);
        if (repair == null) {
            throw new RuntimeException("Repair report not found");
        }

        AiWorkOrderAnalyzeResult result = provider.analyze(repair);
        AiWorkOrderAnalysis analysis = toSnapshot(repair, result);

        store.clearLatest(repairId);
        return store.save(analysis);
    }

    @Override
    public AiWorkOrderAnalysis getLatestAnalysis(Long repairId) {
        Repair repair = repairService.getById(repairId);
        if (repair == null) {
            throw new RuntimeException("Repair report not found");
        }
        return store.findLatestByRepairId(repairId)
                .orElseThrow(() -> new RuntimeException("AI analysis snapshot not found"));
    }

    @Override
    public List<AiWorkOrderAnalysis> getAnalysisHistory(Long repairId) {
        Repair repair = repairService.getById(repairId);
        if (repair == null) {
            throw new RuntimeException("Repair report not found");
        }
        return store.findHistoryByRepairId(repairId);
    }

    private AiWorkOrderAnalysis toSnapshot(Repair repair, AiWorkOrderAnalyzeResult result) {
        AiWorkOrderAnalysis analysis = new AiWorkOrderAnalysis();
        analysis.setRepairId(repair.getId());
        analysis.setCategory(result.getCategory());
        analysis.setPriority(result.getPriority());
        analysis.setUrgencyLevel(result.getUrgencyLevel());
        analysis.setRiskLevel(result.getRiskLevel());
        analysis.setRecommendedTeam(result.getRecommendedTeam());
        analysis.setSuggestedAction(result.getSuggestedAction());
        analysis.setSummary(result.getSummary());
        analysis.setExtractedLocation(result.getExtractedLocation());
        analysis.setSuggestedResponseMinutes(result.getSuggestedResponseMinutes());
        analysis.setSafetyTips(toJson(result.getSafetyTips()));
        analysis.setMatchedKeywords(join(result.getMatchedKeywords()));
        analysis.setConfidence(result.getConfidence());
        analysis.setManualReviewNeeded(result.getManualReviewNeeded());
        analysis.setProvider(result.getProvider());
        analysis.setProviderVersion(result.getProviderVersion());
        analysis.setLatest(true);
        analysis.setCreateTime(LocalDateTime.now());
        return analysis;
    }

    private String join(List<String> values) {
        return values == null ? "" : values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(Collectors.joining(","));
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return "[]";
        }
        List<String> cleanValues = values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(Collectors.toList());
        return JSON.toJSONString(cleanValues);
    }
}
