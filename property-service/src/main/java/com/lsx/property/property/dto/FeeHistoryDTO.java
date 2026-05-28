package com.lsx.property.property.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeeHistoryDTO {
    private Long recordId;           // 记录ID
    private String feeCycle;         // 费用周期
    private String feeType;          // 费用类型
    private BigDecimal payAmount;    // 支付金额
    private String payType;          // 支付方式
    private LocalDateTime payTime;   // 支付时间
    private String status;           // 支付状态
}
