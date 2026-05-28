package com.lsx.property.property.dto;

import lombok.Data;

@Data
public class PayFeeDTO {
    private Long feeId;              // 费用ID
    private String payType;          // 支付方式
}
