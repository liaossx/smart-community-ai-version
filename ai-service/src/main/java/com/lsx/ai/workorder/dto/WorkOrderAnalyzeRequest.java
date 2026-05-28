package com.lsx.ai.workorder.dto;

import jakarta.validation.constraints.NotBlank;

public class WorkOrderAnalyzeRequest {
    private Long repairId;
    private String faultType;

    @NotBlank(message = "faultDesc is required")
    private String faultDesc;

    public Long getRepairId() {
        return repairId;
    }

    public void setRepairId(Long repairId) {
        this.repairId = repairId;
    }

    public String getFaultType() {
        return faultType;
    }

    public void setFaultType(String faultType) {
        this.faultType = faultType;
    }

    public String getFaultDesc() {
        return faultDesc;
    }

    public void setFaultDesc(String faultDesc) {
        this.faultDesc = faultDesc;
    }
}
