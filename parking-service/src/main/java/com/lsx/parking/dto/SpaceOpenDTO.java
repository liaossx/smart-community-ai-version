package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpaceOpenDTO {

    @Schema(description = "车位ID或绑定记录ID", required = true)
    private Long spaceId;

    @Schema(description = "购买时长(月)", required = true)
    private Integer durationMonths;

    @Schema(description = "支付方式 (如 BALANCE)", required = true)
    private String payMethod;

    @Schema(description = "支付金额", required = true)
    private BigDecimal amount;

    @Schema(description = "用户ID (可选，若不传则从Context获取)")
    private Long userId;
}
