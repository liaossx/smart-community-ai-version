package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParkingSpaceBindDTO {

    @Schema(description = "车位ID", required = true)
    private Long spaceId;

    @Schema(description = "业主ID", required = true)
    private Long userId;

    @Schema(description = "房屋ID", required = true)
    private Long houseId;

    @Schema(description = "租赁结束时间（固定车位）")
    private LocalDateTime leaseEndTime;
}
