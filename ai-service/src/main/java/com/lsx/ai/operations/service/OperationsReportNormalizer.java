package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsReportRequest;
import com.lsx.ai.operations.dto.OperationsReportResponse;
import com.lsx.ai.operations.dto.OperationsRiskAlert;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class OperationsReportNormalizer {

    /*
     * 运营周报输出校正器。
     *
     * 负责把模型输出拉回后端需要的稳定结构：
     * 补标题、补居民诉求摘要、规范风险等级、判断是否需要人工复核。
     */
    public OperationsReportResponse normalize(OperationsReportResponse response,
                                              OperationsReportRequest request,
                                              String provider,
                                              String providerVersion,
                                              String model) {
        OperationsReportResponse result = response == null
                ? new OperationsReportResponse()
                : response;
        if (!StringUtils.hasText(result.getReportTitle())) {
            result.setReportTitle(buildDefaultTitle(request));
        }
        if (!StringUtils.hasText(result.getExecutiveSummary())) {
            result.setExecutiveSummary("本期运营数据已汇总，请结合风险提醒和居民诉求安排后续跟进。");
        }
        if (!StringUtils.hasText(result.getResidentAppealSummary())) {
            result.setResidentAppealSummary(buildAppealSummary(request));
        }
        result.setWeeklyHighlights(nonNullStrings(result.getWeeklyHighlights()));
        result.setRecommendedActions(nonNullStrings(result.getRecommendedActions()));
        result.setRiskAlerts(normalizeRisks(result.getRiskAlerts()));
        if (result.getManualReviewNeeded() == null) {
            // 是否人工复核不能完全交给模型；高风险、紧急维修、投诉积压都应触发人工复核。
            result.setManualReviewNeeded(hasHighRisk(result.getRiskAlerts())
                    || defaultInt(request.getUrgentRepairCount()) > 0
                    || defaultInt(request.getComplaintPending()) >= 5);
        }
        result.setConfidence(calibrateConfidence(result.getConfidence(), request, provider));
        result.setProvider(provider);
        result.setProviderVersion(providerVersion);
        result.setModel(model);
        return result;
    }

    private String buildDefaultTitle(OperationsReportRequest request) {
        String communityName = StringUtils.hasText(request.getCommunityName())
                ? request.getCommunityName()
                : "社区";
        return communityName + "运营周报（" + request.getStartDate() + "至" + request.getEndDate() + "）";
    }

    private String buildAppealSummary(OperationsReportRequest request) {
        List<String> appeals = request.getResidentAppeals();
        if (appeals == null || appeals.isEmpty()) {
            return "本期未提供居民诉求明细。";
        }
        return "本期居民诉求主要集中在：" + String.join("；", appeals) + "。";
    }

    private List<String> nonNullStrings(List<String> values) {
        return values == null ? new ArrayList<>() : values;
    }

    private List<OperationsRiskAlert> normalizeRisks(List<OperationsRiskAlert> risks) {
        if (risks == null) {
            return new ArrayList<>();
        }
        for (OperationsRiskAlert risk : risks) {
            if (!StringUtils.hasText(risk.getRiskLevel())) {
                risk.setRiskLevel("LOW");
            }
            risk.setRiskLevel(normalizeRiskLevel(risk.getRiskLevel()));
        }
        return risks;
    }

    private String normalizeRiskLevel(String riskLevel) {
        // 只允许后端约定的风险枚举，模型输出其他词时降级成 LOW。
        String upper = riskLevel == null ? "LOW" : riskLevel.trim().toUpperCase();
        if ("LOW".equals(upper) || "MEDIUM".equals(upper) || "HIGH".equals(upper) || "CRITICAL".equals(upper)) {
            return upper;
        }
        return "LOW";
    }

    private boolean hasHighRisk(List<OperationsRiskAlert> risks) {
        if (risks == null) {
            return false;
        }
        return risks.stream()
                .map(OperationsRiskAlert::getRiskLevel)
                .anyMatch(level -> "HIGH".equals(level) || "CRITICAL".equals(level));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer calibrateConfidence(Integer confidence, OperationsReportRequest request, String provider) {
        // 模型有时会乱给 confidence。这里按 SQL 指标完整度和风险强度拉回合理区间。
        int value = clampConfidence(confidence);
        if (!"SPRING_AI_OPERATIONS".equals(provider)) {
            return value;
        }
        int minimum = hasOperationalSignals(request) ? 85 : 80;
        if (hasHighRiskSignals(request)) {
            minimum = 90;
        }
        return Math.max(value, minimum);
    }

    private boolean hasOperationalSignals(OperationsReportRequest request) {
        return defaultInt(request.getRepairTotal()) > 0
                || defaultInt(request.getComplaintTotal()) > 0
                || defaultInt(request.getVisitorTotal()) > 0
                || defaultInt(request.getFeeUnpaidCount()) > 0
                || defaultInt(request.getNoticePublishedCount()) > 0
                || !isEmpty(request.getTopRepairCategories())
                || !isEmpty(request.getResidentAppeals())
                || !isEmpty(request.getRecentRiskEvents());
    }

    private boolean hasHighRiskSignals(OperationsReportRequest request) {
        return defaultInt(request.getUrgentRepairCount()) > 0
                || defaultInt(request.getComplaintPending()) >= 5
                || !isEmpty(request.getRecentRiskEvents());
    }

    private boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private Integer clampConfidence(Integer confidence) {
        if (confidence == null) {
            return 75;
        }
        if (confidence < 0) {
            return 0;
        }
        if (confidence > 100) {
            return 100;
        }
        return confidence;
    }
}
