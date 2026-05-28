package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpaceRenewDTO {

    @Schema(description = "车位ID", required = true)
    private Long spaceId;

    @Schema(description = "续费月数", required = true, example = "1")
    private Integer durationMonths;

    @Schema(description = "支付方式", example = "BALANCE")
    private String payMethod;

    @Schema(description = "支付金额（后端需二次校验）", required = true)
    private BigDecimal amount;

    @Schema(description = "用户ID（可选，建议从Token取）")
    private Long userId;
}
