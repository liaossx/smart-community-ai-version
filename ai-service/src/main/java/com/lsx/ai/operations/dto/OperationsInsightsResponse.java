package com.lsx.ai.operations.dto;

import java.util.ArrayList;
import java.util.List;

public class OperationsInsightsResponse {
    private String overallRiskLevel;
    private String headline;
    private String summary;
    private List<OperationsInsightCard> insightCards = new ArrayList<>();
    private List<OperationsActionItem> actionItems = new ArrayList<>();
    private Boolean manualReviewNeeded;
    private Integer confidence;
    private String provider;
    private String providerVersion;
    private String model;

    public String getOverallRiskLevel() {
        return overallRiskLevel;
    }

    public void setOverallRiskLevel(String overallRiskLevel) {
        this.overallRiskLevel = overallRiskLevel;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<OperationsInsightCard> getInsightCards() {
        return insightCards;
    }

    public void setInsightCards(List<OperationsInsightCard> insightCards) {
        this.insightCards = insightCards;
    }

    public List<OperationsActionItem> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<OperationsActionItem> actionItems) {
        this.actionItems = actionItems;
    }

    public Boolean getManualReviewNeeded() {
        return manualReviewNeeded;
    }

    public void setManualReviewNeeded(Boolean manualReviewNeeded) {
        this.manualReviewNeeded = manualReviewNeeded;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
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
