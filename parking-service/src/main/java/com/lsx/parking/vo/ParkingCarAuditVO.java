package com.lsx.parking.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ParkingCarAuditVO {
    private Long id;
    private String plateNo;
    private String userName; // 申请人姓名
    private String spaceNo;  // 申请车位号
    private String status;   // PENDING, APPROVED, REJECTED
    private String rejectReason;
    private LocalDateTime createTime;
}
