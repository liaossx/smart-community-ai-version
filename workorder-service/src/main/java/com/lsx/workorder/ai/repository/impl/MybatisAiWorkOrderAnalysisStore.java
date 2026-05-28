package com.lsx.workorder.ai.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.workorder.ai.entity.AiWorkOrderAnalysis;
import com.lsx.workorder.ai.mapper.AiWorkOrderAnalysisMapper;
import com.lsx.workorder.ai.repository.AiWorkOrderAnalysisStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisAiWorkOrderAnalysisStore implements AiWorkOrderAnalysisStore {

    private final AiWorkOrderAnalysisMapper mapper;

    public MybatisAiWorkOrderAnalysisStore(AiWorkOrderAnalysisMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void clearLatest(Long repairId) {
        mapper.update(null, Wrappers.<AiWorkOrderAnalysis>lambdaUpdate()
                .eq(AiWorkOrderAnalysis::getRepairId, repairId)
                .eq(AiWorkOrderAnalysis::getLatest, true)
                .set(AiWorkOrderAnalysis::getLatest, false));
    }

    @Override
    public AiWorkOrderAnalysis save(AiWorkOrderAnalysis analysis) {
        mapper.insert(analysis);
        return analysis;
    }

    @Override
    public Optional<AiWorkOrderAnalysis> findLatestByRepairId(Long repairId) {
        AiWorkOrderAnalysis analysis = mapper.selectOne(Wrappers.<AiWorkOrderAnalysis>lambdaQuery()
                .eq(AiWorkOrderAnalysis::getRepairId, repairId)
                .eq(AiWorkOrderAnalysis::getLatest, true)
                .last("LIMIT 1"));
        return Optional.ofNullable(analysis);
    }

    @Override
    public List<AiWorkOrderAnalysis> findHistoryByRepairId(Long repairId) {
        return mapper.selectList(Wrappers.<AiWorkOrderAnalysis>lambdaQuery()
                .eq(AiWorkOrderAnalysis::getRepairId, repairId)
                .orderByDesc(AiWorkOrderAnalysis::getCreateTime)
                .orderByDesc(AiWorkOrderAnalysis::getId));
    }
}
