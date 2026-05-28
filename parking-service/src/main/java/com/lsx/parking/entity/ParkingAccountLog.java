package com.lsx.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_parking_account_log")
public class ParkingAccountLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountId;

    private Long orderId;

    private BigDecimal amount;

    private String type; // RECHARGE / CONSUME

    private String remark;

    private LocalDateTime createTime;
}
