package com.lsx.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_parking_space_plate")
public class ParkingSpacePlate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spaceId;
    private String plateNo;
    private Long userId;
    private String status;
    private String rejectReason; // 拒绝原因
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

