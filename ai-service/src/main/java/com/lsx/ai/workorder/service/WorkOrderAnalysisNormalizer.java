package com.lsx.ai.workorder.service;

import com.lsx.ai.workorder.dto.WorkOrderAnalyzeResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class WorkOrderAnalysisNormalizer {
    private static final List<String> AMBIGUOUS_KEYWORDS = List.of(
            "不确定", "疑似", "好像", "可能", "麻烦看一下", "看一下", "帮忙看一下",
            "not sure", "maybe", "possible", "suspect", "check it"
    );

    private static final List<String> HIGH_RISK_KEYWORDS = List.of(
            "困人", "被困", "冒烟", "起火", "燃气", "煤气", "漏电", "焦糊味",
            "爆管", "大面积积水", "无法进出", "trapped", "smoke", "fire",
            "gas", "electric leak", "electric shock", "burning smell", "burst", "flooding"
    );

    public WorkOrderAnalyzeResponse normalize(WorkOrderAnalyzeResponse response, String providerVersion, String model) {
        WorkOrderAnalyzeResponse result = response == null ? new WorkOrderAnalyzeResponse() : response;
        Integer priority = clampPriority(result.getPriority());
        priority = calibratePriority(priority, result.getSuggestedResponseMinutes());
        priority = softenAmbiguousHighRisk(priority, result);
        result.setPriority(priority);
        result.setPriorityDesc(priorityDesc(priority));
        result.setUrgencyLevel(urgencyLevel(priority));
        result.setRiskLevel(riskLevel(priority));
        if (result.getSuggestedResponseMinutes() == null) {
            result.setSuggestedResponseMinutes(suggestedResponseMinutes(priority));
        }
        if (result.getConfidence() == null) {
            result.setConfidence(70);
        }
        if (priority >= 4) {
            result.setManualReviewNeeded(true);
        } else if (isAmbiguous(result)) {
            result.setManualReviewNeeded(true);
        } else if (result.getManualReviewNeeded() == null) {
            result.setManualReviewNeeded(result.getConfidence() < 75 || priority >= 4);
        }
        if (result.getMatchedKeywords() == null) {
            result.setMatchedKeywords(new ArrayList<>());
        }
        if (result.getSafetyTips() == null) {
            result.setSafetyTips(new ArrayList<>());
        }
        result.setProvider("SPRING_AI");
        result.setProviderVersion(providerVersion);
        result.setModel(model);
        return result;
    }

    private Integer clampPriority(Integer priority) {
        if (priority == null) {
            return 1;
        }
        if (priority < 1) {
            return 1;
        }
        if (priority > 4) {
            return 4;
        }
        return priority;
    }

    private Integer calibratePriority(Integer priority, Integer suggestedResponseMinutes) {
        int calibrated = priority;
        if (suggestedResponseMinutes != null) {
            if (suggestedResponseMinutes <= 15) {
                calibrated = Math.max(calibrated, 4);
            } else if (suggestedResponseMinutes <= 30) {
                calibrated = Math.max(calibrated, 3);
            } else if (suggestedResponseMinutes <= 60) {
                calibrated = Math.max(calibrated, 2);
            }
        }
        return calibrated;
    }

    private Integer softenAmbiguousHighRisk(Integer priority, WorkOrderAnalyzeResponse result) {
        if (priority == null || priority < 4) {
            return priority;
        }
        String combinedText = combinedText(result);
        if (containsAny(combinedText, AMBIGUOUS_KEYWORDS) && !containsAny(combinedText, HIGH_RISK_KEYWORDS)) {
            return 3;
        }
        return priority;
    }

    private boolean isAmbiguous(WorkOrderAnalyzeResponse result) {
        return containsAny(combinedText(result), AMBIGUOUS_KEYWORDS);
    }

    private String combinedText(WorkOrderAnalyzeResponse result) {
        return normalizeText(result.getSummary())
                + " " + normalizeText(result.getSuggestedAction())
                + " " + normalizeText(result.getExtractedLocation())
                + " " + normalizeList(result.getMatchedKeywords());
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.toLowerCase(Locale.ROOT) : "";
    }

    private String normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                builder.append(value.toLowerCase(Locale.ROOT)).append(' ');
            }
        }
        return builder.toString();
    }

    private String priorityDesc(int priority) {
        switch (priority) {
            case 4:
                return "高危特急";
            case 3:
                return "紧急";
            case 2:
                return "较急";
            default:
                return "普通";
        }
    }

    private String urgencyLevel(int priority) {
        switch (priority) {
            case 4:
                return "CRITICAL";
            case 3:
                return "HIGH";
            case 2:
                return "MEDIUM";
            default:
                return "LOW";
        }
    }

    private String riskLevel(int priority) {
        if (priority >= 4) {
            return "HIGH";
        }
        if (priority >= 3) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Integer suggestedResponseMinutes(int priority) {
        switch (priority) {
            case 4:
                return 15;
            case 3:
                return 30;
            case 2:
                return 60;
            default:
                return 120;
        }
    }
}
