package com.lsx.ai.operations.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * A shared operations metrics snapshot used by both weekly reports and risk insights.
 */
public class OperationsMetricsSnapshot {
    private Long communityId;
    private String communityName;

    @NotBlank(message = "startDate is required")
    private String startDate;

    @NotBlank(message = "endDate is required")
    private String endDate;

    private Integer repairTotal;
    private Integer repairPending;
    private Integer repairCompleted;
    private Integer urgentRepairCount;
    private Integer complaintTotal;
    private Integer complaintPending;
    private Integer visitorTotal;
    private Integer feeUnpaidCount;
    private Integer noticePublishedCount;
    private List<String> topRepairCategories = new ArrayList<>();
    private List<String> residentAppeals = new ArrayList<>();
    private List<String> recentRiskEvents = new ArrayList<>();

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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getRepairTotal() {
        return repairTotal;
    }

    public void setRepairTotal(Integer repairTotal) {
        this.repairTotal = repairTotal;
    }

    public Integer getRepairPending() {
        return repairPending;
    }

    public void setRepairPending(Integer repairPending) {
        this.repairPending = repairPending;
    }

    public Integer getRepairCompleted() {
        return repairCompleted;
    }

    public void setRepairCompleted(Integer repairCompleted) {
        this.repairCompleted = repairCompleted;
    }

    public Integer getUrgentRepairCount() {
        return urgentRepairCount;
    }

    public void setUrgentRepairCount(Integer urgentRepairCount) {
        this.urgentRepairCount = urgentRepairCount;
    }

    public Integer getComplaintTotal() {
        return complaintTotal;
    }

    public void setComplaintTotal(Integer complaintTotal) {
        this.complaintTotal = complaintTotal;
    }

    public Integer getComplaintPending() {
        return complaintPending;
    }

    public void setComplaintPending(Integer complaintPending) {
        this.complaintPending = complaintPending;
    }

    public Integer getVisitorTotal() {
        return visitorTotal;
    }

    public void setVisitorTotal(Integer visitorTotal) {
        this.visitorTotal = visitorTotal;
    }

    public Integer getFeeUnpaidCount() {
        return feeUnpaidCount;
    }

    public void setFeeUnpaidCount(Integer feeUnpaidCount) {
        this.feeUnpaidCount = feeUnpaidCount;
    }

    public Integer getNoticePublishedCount() {
        return noticePublishedCount;
    }

    public void setNoticePublishedCount(Integer noticePublishedCount) {
        this.noticePublishedCount = noticePublishedCount;
    }

    public List<String> getTopRepairCategories() {
        return topRepairCategories;
    }

    public void setTopRepairCategories(List<String> topRepairCategories) {
        this.topRepairCategories = topRepairCategories;
    }

    public List<String> getResidentAppeals() {
        return residentAppeals;
    }

    public void setResidentAppeals(List<String> residentAppeals) {
        this.residentAppeals = residentAppeals;
    }

    public List<String> getRecentRiskEvents() {
        return recentRiskEvents;
    }

    public void setRecentRiskEvents(List<String> recentRiskEvents) {
        this.recentRiskEvents = recentRiskEvents;
    }
}
