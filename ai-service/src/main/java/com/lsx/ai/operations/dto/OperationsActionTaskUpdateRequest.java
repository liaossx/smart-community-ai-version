package com.lsx.ai.operations.dto;

import jakarta.validation.constraints.NotBlank;

public class OperationsActionTaskUpdateRequest {
    @NotBlank(message = "status is required")
    private String status;
    private String feedbackResult;
    private String feedbackBy;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFeedbackResult() {
        return feedbackResult;
    }

    public void setFeedbackResult(String feedbackResult) {
        this.feedbackResult = feedbackResult;
    }

    public String getFeedbackBy() {
        return feedbackBy;
    }

    public void setFeedbackBy(String feedbackBy) {
        this.feedbackBy = feedbackBy;
    }
}
