package com.lsx.property.property.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GenerateFeeDTO {
    private String communityName;    // 社区名称
    private String buildingNo;       // 楼栋号
    private String feeCycle;         // 费用周期(如2025-01)
    private String dueDate;          // 截止日期
    private BigDecimal unitPrice;    // 单价
}
