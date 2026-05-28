package com.lsx.ai.workorder.dto;

import java.util.ArrayList;
import java.util.List;

public class WorkOrderAnalyzeResponse {
    private String category;
    private Integer priority;
    private String priorityDesc;
    private String urgencyLevel;
    private String riskLevel;
    private String recommendedTeam;
    private String suggestedAction;
    private String summary;
    private String extractedLocation;
    private Integer suggestedResponseMinutes;
    private List<String> matchedKeywords = new ArrayList<>();
    private List<String> safetyTips = new ArrayList<>();
    private Integer confidence;
    private Boolean manualReviewNeeded;
    private String provider;
    private String providerVersion;
    private String model;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getPriorityDesc() {
        return priorityDesc;
    }

    public void setPriorityDesc(String priorityDesc) {
        this.priorityDesc = priorityDesc;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRecommendedTeam() {
        return recommendedTeam;
    }

    public void setRecommendedTeam(String recommendedTeam) {
        this.recommendedTeam = recommendedTeam;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getExtractedLocation() {
        return extractedLocation;
    }

    public void setExtractedLocation(String extractedLocation) {
        this.extractedLocation = extractedLocation;
    }

    public Integer getSuggestedResponseMinutes() {
        return suggestedResponseMinutes;
    }

    public void setSuggestedResponseMinutes(Integer suggestedResponseMinutes) {
        this.suggestedResponseMinutes = suggestedResponseMinutes;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords;
    }

    public List<String> getSafetyTips() {
        return safetyTips;
    }

    public void setSafetyTips(List<String> safetyTips) {
        this.safetyTips = safetyTips;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Boolean getManualReviewNeeded() {
        return manualReviewNeeded;
    }

    public void setManualReviewNeeded(Boolean manualReviewNeeded) {
        this.manualReviewNeeded = manualReviewNeeded;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public void setProviderVersion(String providerVersion) {
        this.providerVersion = providerVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
