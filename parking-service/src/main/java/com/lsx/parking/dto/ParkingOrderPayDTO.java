package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingOrderPayDTO {

    @Schema(description = "业主ID", required = true)
    private Long userId;

    @Schema(description = "支付渠道（WECHAT/ALIPAY/CASH）", required = true)
    private String payChannel;

    @Schema(description = "支付备注/凭证")
    private String payRemark;
}
