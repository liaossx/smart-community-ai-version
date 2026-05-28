package com.lsx.house.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HouseBindRequestDTO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String communityName;
    private String buildingNo;
    private String houseNo;
    private String identityType;
    private String status;
    private LocalDateTime applyTime;
    private String rejectReason;
}
