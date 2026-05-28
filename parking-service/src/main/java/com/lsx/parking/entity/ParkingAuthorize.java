package com.lsx.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_parking_authorize")
public class ParkingAuthorize {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long spaceId;
    private Long userId;
    private String authorizedName;
    private String authorizedPhone;
    private String plateNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // ACTIVE / EXPIRED / REVOKED
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}









