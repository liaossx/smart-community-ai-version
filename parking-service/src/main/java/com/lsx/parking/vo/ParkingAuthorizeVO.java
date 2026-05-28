package com.lsx.parking.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParkingAuthorizeVO {
    private Long id;
    private Long spaceId;
    private String spaceNo;
    private String authorizedName;
    private String authorizedPhone;
    private String plateNo;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}









