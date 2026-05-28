package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParkingReserveCreateDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "车位ID")
    private Long spaceId;

    @Schema(description = "预约开始时间")
    private LocalDateTime reserveStartTime;

    @Schema(description = "预约结束时间")
    private LocalDateTime reserveEndTime;
}
