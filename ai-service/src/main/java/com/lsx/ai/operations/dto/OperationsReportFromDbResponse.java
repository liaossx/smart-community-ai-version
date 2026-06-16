package com.lsx.ai.operations.dto;

public class OperationsReportFromDbResponse {
    private String source;
    private OperationsMetricsSnapshot sourceData;
    private OperationsReportResponse report;

    public OperationsReportFromDbResponse() {
    }

    public OperationsReportFromDbResponse(String source,
                                          OperationsMetricsSnapshot sourceData,
                                          OperationsReportResponse report) {
        this.source = source;
        this.sourceData = sourceData;
        this.report = report;
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

    public OperationsReportResponse getReport() {
        return report;
    }

    public void setReport(OperationsReportResponse report) {
        this.report = report;
    }
}

