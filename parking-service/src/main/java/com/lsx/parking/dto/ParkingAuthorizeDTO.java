package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParkingAuthorizeDTO {

    @Schema(description = "业主ID", required = true)
    private Long userId;

    @Schema(description = "授权人姓名", required = true)
    private String authorizedName;

    @Schema(description = "授权人手机号", required = true)
    private String authorizedPhone;

    @Schema(description = "授权车牌号")
    private String plateNo;

    @Schema(description = "授权结束时间", required = true)
    private LocalDateTime endTime;
}
