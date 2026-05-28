package com.lsx.ai.operations.dto;

import java.util.ArrayList;
import java.util.List;

public class OperationsReportResponse {
    private String reportTitle;
    private String executiveSummary;
    private List<String> weeklyHighlights = new ArrayList<>();
    private List<OperationsRiskAlert> riskAlerts = new ArrayList<>();
    private String residentAppealSummary;
    private List<String> recommendedActions = new ArrayList<>();
    private Boolean manualReviewNeeded;
    private Integer confidence;
    private String provider;
    private String providerVersion;
    private String model;

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }

    public List<String> getWeeklyHighlights() {
        return weeklyHighlights;
    }

    public void setWeeklyHighlights(List<String> weeklyHighlights) {
        this.weeklyHighlights = weeklyHighlights;
    }

    public List<OperationsRiskAlert> getRiskAlerts() {
        return riskAlerts;
    }

    public void setRiskAlerts(List<OperationsRiskAlert> riskAlerts) {
        this.riskAlerts = riskAlerts;
    }

    public String getResidentAppealSummary() {
        return residentAppealSummary;
    }

    public void setResidentAppealSummary(String residentAppealSummary) {
        this.residentAppealSummary = residentAppealSummary;
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(List<String> recommendedActions) {
        this.recommendedActions = recommendedActions;
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
