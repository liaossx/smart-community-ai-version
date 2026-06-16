package com.lsx.ai.operations.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OperationsActionTaskItem {
    private Long id;
    private String taskBatchNo;
    private Long communityId;
    private String communityName;
    private String source;
    private String sourceVersion;
    private LocalDate sourceWindowStart;
    private LocalDate sourceWindowEnd;
    private String overallRiskLevel;
    private String priority;
    private String ownerRole;
    private String taskTitle;
    private String taskReason;
    private String deadlineText;
    private String status;
    private String feedbackResult;
    private String feedbackBy;
    private LocalDateTime feedbackTime;
    private Boolean closedLoop;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskBatchNo() {
        return taskBatchNo;
    }

    public void setTaskBatchNo(String taskBatchNo) {
        this.taskBatchNo = taskBatchNo;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public LocalDate getSourceWindowStart() {
        return sourceWindowStart;
    }

    public void setSourceWindowStart(LocalDate sourceWindowStart) {
        this.sourceWindowStart = sourceWindowStart;
    }

    public LocalDate getSourceWindowEnd() {
        return sourceWindowEnd;
    }

    public void setSourceWindowEnd(LocalDate sourceWindowEnd) {
        this.sourceWindowEnd = sourceWindowEnd;
    }

    public String getOverallRiskLevel() {
        return overallRiskLevel;
    }

    public void setOverallRiskLevel(String overallRiskLevel) {
        this.overallRiskLevel = overallRiskLevel;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getOwnerRole() {
        return ownerRole;
    }

    public void setOwnerRole(String ownerRole) {
        this.ownerRole = ownerRole;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getTaskReason() {
        return taskReason;
    }

    public void setTaskReason(String taskReason) {
        this.taskReason = taskReason;
    }

    public String getDeadlineText() {
        return deadlineText;
    }

    public void setDeadlineText(String deadlineText) {
        this.deadlineText = deadlineText;
    }

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

    public LocalDateTime getFeedbackTime() {
        return feedbackTime;
    }

    public void setFeedbackTime(LocalDateTime feedbackTime) {
        this.feedbackTime = feedbackTime;
    }

    public Boolean getClosedLoop() {
        return closedLoop;
    }

    public void setClosedLoop(Boolean closedLoop) {
        this.closedLoop = closedLoop;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
