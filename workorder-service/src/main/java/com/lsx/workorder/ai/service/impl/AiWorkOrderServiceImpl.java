package com.lsx.workorder.ai.service.impl;

import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeRequest;
import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.ai.provider.AiWorkOrderAnalysisProvider;
import com.lsx.workorder.ai.service.AiWorkOrderService;
import com.lsx.workorder.repair.entity.Repair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiWorkOrderServiceImpl implements AiWorkOrderService {

    private final AiWorkOrderAnalysisProvider provider;

    public AiWorkOrderServiceImpl(AiWorkOrderAnalysisProvider provider) {
        this.provider = provider;
    }

    @Override
    public AiWorkOrderAnalyzeResult analyze(AiWorkOrderAnalyzeRequest request) {
        Repair repair = new Repair();
        if (request != null) {
            repair.setFaultType(request.getFaultType());
            repair.setFaultDesc(buildFaultDesc(request));
        }
        return provider.analyze(repair);
    }

    @Override
    public AiWorkOrderAnalyzeResult analyzeRepair(Repair repair) {
        return provider.analyze(repair);
    }

    private String buildFaultDesc(AiWorkOrderAnalyzeRequest request) {
        StringBuilder sb = new StringBuilder();
        append(sb, request.getFaultDesc());
        append(sb, request.getAddress());
        return sb.toString();
    }

    private void append(StringBuilder sb, String value) {
        if (StringUtils.hasText(value)) {
            sb.append(value).append(' ');
        }
    }
}
