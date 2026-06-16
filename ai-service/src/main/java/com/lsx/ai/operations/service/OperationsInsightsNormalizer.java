package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsActionItem;
import com.lsx.ai.operations.dto.OperationsInsightCard;
import com.lsx.ai.operations.dto.OperationsInsightsResponse;
import com.lsx.ai.operations.dto.OperationsMetricsSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class OperationsInsightsNormalizer {

    public OperationsInsightsResponse normalize(OperationsInsightsResponse response,
                                                OperationsMetricsSnapshot request,
                                                String provider,
                                                String providerVersion,
                                                String model) {
        OperationsInsightsResponse result = response == null
                ? new OperationsInsightsResponse()
                : response;
        result.setInsightCards(normalizeCards(result.getInsightCards()));
        result.setActionItems(normalizeActions(result.getActionItems()));
        if (!StringUtils.hasText(result.getOverallRiskLevel())) {
            result.setOverallRiskLevel(OperationsRiskAssessment.overallRiskLevel(request, result.getInsightCards()));
        } else {
            result.setOverallRiskLevel(normalizeRiskLevel(result.getOverallRiskLevel()));
        }
        if (!StringUtils.hasText(result.getHeadline())) {
            result.setHeadline(buildHeadline(request, result.getOverallRiskLevel()));
        }
        if (!StringUtils.hasText(result.getSummary())) {
            result.setSummary(buildSummary(request));
        }
        if (result.getManualReviewNeeded() == null) {
            result.setManualReviewNeeded(OperationsRiskAssessment.needsManualReview(request, result.getOverallRiskLevel()));
        }
        result.setConfidence(calibrateConfidence(result.getConfidence(), provider, request));
        result.setProvider(provider);
        result.setProviderVersion(providerVersion);
        result.setModel(model);
        return result;
    }

    private List<OperationsInsightCard> normalizeCards(List<OperationsInsightCard> cards) {
        if (cards == null) {
            return new ArrayList<>();
        }
        cards = new ArrayList<>(cards);
        cards.removeIf(card -> card == null || !StringUtils.hasText(card.getTitle()));
        for (OperationsInsightCard card : cards) {
            if (!StringUtils.hasText(card.getInsightType())) {
                card.setInsightType("GENERAL");
            }
            card.setRiskLevel(normalizeRiskLevel(card.getRiskLevel()));
            if (card.getEvidence() == null) {
                card.setEvidence(new ArrayList<>());
            }
            if (!StringUtils.hasText(card.getAnalysis())) {
                card.setAnalysis("该指标存在运营关注价值，建议结合业务明细继续复核。");
            }
            if (!StringUtils.hasText(card.getRecommendedAction())) {
                card.setRecommendedAction("安排物业运营人员跟进，并在下次周会复盘处理结果。");
            }
        }
        cards.sort(Comparator.comparingInt(card -> riskWeight(card.getRiskLevel())));
        return cards;
    }

    private List<OperationsActionItem> normalizeActions(List<OperationsActionItem> actions) {
        if (actions == null) {
            return new ArrayList<>();
        }
        actions = new ArrayList<>(actions);
        actions.removeIf(action -> action == null || !StringUtils.hasText(action.getTask()));
        for (OperationsActionItem action : actions) {
            if (!StringUtils.hasText(action.getPriority())) {
                action.setPriority("P2");
            }
            action.setPriority(normalizePriority(action.getPriority()));
            if (!StringUtils.hasText(action.getOwnerRole())) {
                action.setOwnerRole("物业运营负责人");
            }
            if (!StringUtils.hasText(action.getDeadline())) {
                action.setDeadline("本周内");
            }
            if (!StringUtils.hasText(action.getReason())) {
                action.setReason("用于降低运营风险并提升居民响应效率。");
            }
        }
        actions.sort(Comparator.comparingInt(action -> priorityWeight(action.getPriority())));
        return actions;
    }

    private String buildHeadline(OperationsMetricsSnapshot request, String riskLevel) {
        String communityName = StringUtils.hasText(request.getCommunityName())
                ? request.getCommunityName()
                : "社区";
        if ("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel)) {
            return communityName + "存在需要优先处理的运营风险";
        }
        if ("MEDIUM".equals(riskLevel)) {
            return communityName + "存在可优化的运营关注点";
        }
        return communityName + "本期运营整体平稳";
    }

    private String buildSummary(OperationsMetricsSnapshot request) {
        return "本期报修" + defaultInt(request.getRepairTotal())
                + "单，投诉" + defaultInt(request.getComplaintTotal())
                + "件，待处理投诉" + defaultInt(request.getComplaintPending())
                + "件，欠费户数" + defaultInt(request.getFeeUnpaidCount())
                + "户。";
    }

    private Integer calibrateConfidence(Integer confidence, String provider, OperationsMetricsSnapshot request) {
        int value = clampConfidence(confidence);
        if (!"SPRING_AI_OPERATIONS_INSIGHTS".equals(provider)) {
            return value;
        }
        int minimum = OperationsRiskAssessment.hasOperationalSignals(request) ? 86 : 78;
        if (OperationsRiskAssessment.hasHighRiskSignals(request)) {
            minimum = 90;
        }
        return Math.max(value, minimum);
    }

    private String normalizeRiskLevel(String riskLevel) {
        String upper = riskLevel == null ? "LOW" : riskLevel.trim().toUpperCase();
        if ("LOW".equals(upper) || "MEDIUM".equals(upper) || "HIGH".equals(upper) || "CRITICAL".equals(upper)) {
            return upper;
        }
        return "LOW";
    }

    private String normalizePriority(String priority) {
        String upper = priority == null ? "P2" : priority.trim().toUpperCase();
        if ("P0".equals(upper) || "P1".equals(upper) || "P2".equals(upper) || "P3".equals(upper)) {
            return upper;
        }
        return "P2";
    }

    private int riskWeight(String riskLevel) {
        if ("CRITICAL".equals(riskLevel)) {
            return 0;
        }
        if ("HIGH".equals(riskLevel)) {
            return 1;
        }
        if ("MEDIUM".equals(riskLevel)) {
            return 2;
        }
        return 3;
    }

    private int priorityWeight(String priority) {
        if ("P0".equals(priority)) {
            return 0;
        }
        if ("P1".equals(priority)) {
            return 1;
        }
        if ("P2".equals(priority)) {
            return 2;
        }
        return 3;
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

    private boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}

