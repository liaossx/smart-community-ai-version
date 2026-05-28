package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class VehicleBindDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "车牌号", required = true, example = "粤A88888")
    private String plateNo;

    @Schema(description = "车辆品牌", example = "奔驰")
    private String brand;

    @Schema(description = "车辆颜色", example = "黑色")
    private String color;

    @Schema(description = "申请绑定的车位ID", required = true)
    private Long spaceId;
}
