package com.lsx.workorder.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiWorkOrderAnalyzeResult {
    private String category;
    private Integer priority;
    private String priorityDesc;
    private String urgencyLevel;
    private String riskLevel;
    private String recommendedTeam;
    private String suggestedAction;
    private String summary;
    private String extractedLocation;
    private Integer estimatedMinutes;
    private Integer suggestedResponseMinutes;
    private List<String> matchedKeywords;
    private List<String> safetyTips;
    private Integer confidence;
    private Boolean manualReviewNeeded;
    private String provider;
    private String providerVersion;
}
