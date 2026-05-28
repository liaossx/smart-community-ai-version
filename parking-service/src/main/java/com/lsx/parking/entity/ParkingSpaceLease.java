package com.lsx.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_parking_space_lease")
public class ParkingSpaceLease {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long spaceId;
    private Long userId;
    private Long houseId;

    /**
     * MONTHLY / YEARLY / PERPETUAL
     */
    private String leaseType;

    private LocalDateTime startTime;

    /**
     * 姘镐箙车位 = NULL
     */
    private LocalDateTime endTime;

    /**
     * ACTIVE / EXPIRED / TERMINATED
     */
    private String status;

    /**
     * 鍏宠仈订单锛堢敤浜庣画璐广€佸璐︼級
     */
    private Long sourceOrderId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
