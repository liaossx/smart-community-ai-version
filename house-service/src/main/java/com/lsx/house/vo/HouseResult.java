package com.lsx.house.vo;

import lombok.Data;

@Data
public class HouseResult {
    private Long id;
    private String communityName;
    private String buildingNo;
    private String houseNo;
    private Integer floor;
    private Double area;
    private String type;
    private Integer bindStatus;
}
