package com.lsx.ai.operations.dto;

public class OperationsReportFromDbResponse {
    private String source;
    private OperationsReportRequest sourceData;
    private OperationsReportResponse report;

    public OperationsReportFromDbResponse() {
    }

    public OperationsReportFromDbResponse(String source,
                                          OperationsReportRequest sourceData,
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

    public OperationsReportRequest getSourceData() {
        return sourceData;
    }

    public void setSourceData(OperationsReportRequest sourceData) {
        this.sourceData = sourceData;
    }

    public OperationsReportResponse getReport() {
        return report;
    }

    public void setReport(OperationsReportResponse report) {
        this.report = report;
    }
}
