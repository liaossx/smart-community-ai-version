package com.lsx.workorder.repair.dto;

import lombok.Data;

/**
 * 工单分配 DTO
 */
@Data
public class WorkOrderAssignDTO {
    private Long orderId;
    private Long workerId;
    private String workerName;
    private String workerPhone;
    private Integer priority;
}
