package com.lsx.system.dto.external;

import lombok.Data;

@Data
public class HouseDTO {
    private Long id;
    private String communityName;
    private String buildingNo;
    private String houseNo;
    private Long communityId;
}
