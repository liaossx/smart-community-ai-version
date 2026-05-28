package com.lsx.workorder.repair.dto;

import lombok.Data;

@Data
public class RepairInfoDTO {
    private Long repairId;
    private String faultDesc;
    private String faultType;
    private String address;
    private String ownerPhone;
    private String ownerName;
    private String buildingNo;
    private String houseNo;
}

