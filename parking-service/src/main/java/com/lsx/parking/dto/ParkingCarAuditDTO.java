package com.lsx.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingCarAuditDTO {

    @Schema(description = "申请记录ID", required = true)
    private Long id;

    @Schema(description = "瀹℃牳状态€侊細APPROVED-閫氳繃锛孯EJECTED-拒绝", required = true)
    private String status;

    @Schema(description = "拒绝原因锛堝綋状态€佷负REJECTED鏃跺繀濉級")
    private String rejectReason;
}

