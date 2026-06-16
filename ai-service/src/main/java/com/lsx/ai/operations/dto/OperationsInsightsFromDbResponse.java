package com.lsx.ai.operations.dto;

public class OperationsInsightsFromDbResponse {
    private String source;
    private OperationsMetricsSnapshot sourceData;
    private OperationsInsightsResponse insights;

    public OperationsInsightsFromDbResponse() {
    }

    public OperationsInsightsFromDbResponse(String source,
                                            OperationsMetricsSnapshot sourceData,
                                            OperationsInsightsResponse insights) {
        this.source = source;
        this.sourceData = sourceData;
        this.insights = insights;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OperationsMetricsSnapshot getSourceData() {
        return sourceData;
    }

    public void setSourceData(OperationsMetricsSnapshot sourceData) {
        this.sourceData = sourceData;
    }

    public OperationsInsightsResponse getInsights() {
        return insights;
    }

    public void setInsights(OperationsInsightsResponse insights) {
        this.insights = insights;
    }
}

