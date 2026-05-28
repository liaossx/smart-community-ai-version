package com.lsx.property.property.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CurrentFeeDTO {
    private Long feeId;              // 费用ID
    private String communityName;    // 社区名称
    private String buildingNo;       // 楼栋号
    private String houseNo;          // 房屋号
    private BigDecimal area;         // 面积
    private String feeCycle;         // 费用周期
    private BigDecimal feeAmount;    // 费用金额
    private LocalDateTime dueDate;   // 截止日期
}
