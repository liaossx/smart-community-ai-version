package com.lsx.ai.workorder.service;

import com.lsx.ai.workorder.dto.WorkOrderAnalyzeResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

@Component
public class WorkOrderAnalysisNormalizer {

    public WorkOrderAnalyzeResponse normalize(WorkOrderAnalyzeResponse response, String providerVersion, String model) {
        WorkOrderAnalyzeResponse result = response == null ? new WorkOrderAnalyzeResponse() : response;
        Integer priority = clampPriority(result.getPriority());
        priority = calibratePriority(priority, result.getSuggestedResponseMinutes());
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
