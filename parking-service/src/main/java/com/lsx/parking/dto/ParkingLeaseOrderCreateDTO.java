package com.lsx.parking.dto;

import lombok.Data;

@Data
public class ParkingLeaseOrderCreateDTO {

    private Long userId;
    private Long spaceId;

    /**
     * MONTHLY / YEARLY / PERPETUAL
     */
    private String leaseType;

    /**
     * 车牌号 (可选，首次办理时用于绑定)
     */
    private String plateNo;
}
