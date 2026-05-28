package com.lsx.property.dto.external;

import lombok.Data;
import java.io.Serializable;

@Data
public class HouseDTO implements Serializable {
    private Long id;
    private String communityName;
    private String buildingNo;
    private String houseNo;
    private Long communityId;
    private Double area;
    private Integer floor;
    private String type;
}
