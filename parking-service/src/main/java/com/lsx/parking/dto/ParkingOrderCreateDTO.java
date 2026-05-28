package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ParkingOrderCreateDTO {

    @Schema(description = "业主ID", required = true)
    private Long userId;

    @Schema(description = "车位ID（固定车位订单需传，传了会自动根据车位类型设置订单类型）")
    private Long spaceId;

    @Schema(description = "订单类型 TEMP/FIXED（传了spaceId时会自动从车位获取，临时订单可不传默认为TEMP）")
    private String orderType;

    @Schema(description = "订单金额", required = true)
    private BigDecimal amount;

    @Schema(description = "预计开始时间")
    private LocalDateTime startTime;

    @Schema(description = "预计结束时间")
    private LocalDateTime endTime;

    @Schema(description = "车牌号 备注")
    private String remark;
}
