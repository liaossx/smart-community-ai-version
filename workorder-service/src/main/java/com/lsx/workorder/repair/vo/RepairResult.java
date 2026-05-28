package com.lsx.workorder.repair.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepairResult {
    private Long id;
    private String communityName;
    private String buildingNo;
    private String houseNo;
    private String faultType;
    private String faultDesc;
    private List<String> faultImgs;
    private String status;
    private String statusDesc;
    private String handleRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
