package com.lsx.workorder.repair.vo;

import lombok.Data;

@Data
public class RepairStatsResult {
    private Integer total;
    private Integer pending;
    private Integer processing;
    private Integer completed;
    private Integer cancelled;
}
