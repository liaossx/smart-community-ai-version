package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingReserveAdminCancelDTO {

    @Schema(description = "预订ID")
    private Long reserveId;

    @Schema(description = "取消原因")
    private String reason;
}
