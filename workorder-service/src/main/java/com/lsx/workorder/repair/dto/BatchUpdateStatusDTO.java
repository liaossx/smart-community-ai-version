package com.lsx.workorder.repair.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchUpdateStatusDTO {
    private List<Long> repairIds;
    private String status;
    private String remark;
}
