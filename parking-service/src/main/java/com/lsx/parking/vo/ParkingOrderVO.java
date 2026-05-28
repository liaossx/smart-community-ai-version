package com.lsx.parking.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ParkingOrderVO {
    private Long orderId;
    private String orderNo;
    private String orderType;
    private BigDecimal amount;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime payTime;
    private String payChannel;
    private String payRemark;
}









