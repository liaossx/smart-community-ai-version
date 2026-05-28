package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingSpaceQueryDTO {

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "页大小", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "车位编号")
    private String spaceNo;

    @Schema(description = "车位状态 (AVAILABLE/DISABLED)")
    private String status;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "社区ID（管理员端按社区隔离）")
    private Long communityId;
}
