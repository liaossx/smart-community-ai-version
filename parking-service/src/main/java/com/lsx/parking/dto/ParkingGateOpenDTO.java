package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingGateOpenDTO {

    @Schema(description = "业主ID", required = true)
    private Long userId;

    @Schema(description = "车位ID", required = true)
    private Long spaceId;
}
