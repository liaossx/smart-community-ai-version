package com.lsx.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_parking_order")
public class ParkingOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long communityId;
    private String orderNo;
    private Long userId;
    private Long spaceId;
    private String plateNo;
    /**
     * TEMP / FIXED
     */
    private String orderType;
    private BigDecimal amount;
    private String status; // UNPAID / PAID / CANCELLED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime payTime;
    private String payChannel;
    private String tradeNo;
    private BigDecimal paidAmount;
    private String payRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}









