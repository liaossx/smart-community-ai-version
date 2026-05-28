package com.lsx.parking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_parking_lease_order")
public class ParkingLeaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long spaceId;
    private String leaseType;
    private BigDecimal amount;
    private String status;
    private LocalDateTime payTime;
    private String payChannel;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
