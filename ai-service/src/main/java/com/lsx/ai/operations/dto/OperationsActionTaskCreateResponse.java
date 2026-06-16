package com.lsx.ai.operations.dto;

import java.util.ArrayList;
import java.util.List;

public class OperationsActionTaskCreateResponse {
    private String taskBatchNo;
    private String source;
    private OperationsMetricsSnapshot sourceData;
    private OperationsInsightsResponse insights;
    private Integer createdCount;
    private List<OperationsActionTaskItem> tasks = new ArrayList<>();

    public String getTaskBatchNo() {
        return taskBatchNo;
    }

    public void setTaskBatchNo(String taskBatchNo) {
        this.taskBatchNo = taskBatchNo;
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

    public Integer getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Integer createdCount) {
        this.createdCount = createdCount;
    }

    public List<OperationsActionTaskItem> getTasks() {
        return tasks;
    }

    public void setTasks(List<OperationsActionTaskItem> tasks) {
        this.tasks = tasks;
    }
}
