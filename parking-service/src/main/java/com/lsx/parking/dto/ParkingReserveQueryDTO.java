package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingReserveQueryDTO {

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "页大小", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "预约状态 RESERVED/EXPIRED/CANCELLED")
    private String status;

    @Schema(description = "车位编号（管理员查询用）")
    private String spaceNo;

    @Schema(description = "用户ID（管理员查询用）")
    private Long userId;
}
