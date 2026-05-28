package com.lsx.workorder.repair.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepairDto {
    private Long id;
    private Long userId;
    private String buildingNo;
    private String houseNo;
    private String faultType;
    private String faultDesc;
    private String faultImgs;
    private String status;
    private String handleRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
