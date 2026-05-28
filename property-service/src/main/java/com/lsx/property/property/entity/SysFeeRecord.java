package com.lsx.property.property.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_fee_record")
public class SysFeeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 主键ID
    private Long feeId;              // 费用ID
    private Long userId;             // 用户ID
    private Long houseId;            // 房屋ID
    private BigDecimal payAmount;    // 支付金额
    private String payType;          // 支付方式 WECHAT/ALIPAY/CASH
    private LocalDateTime payTime;   // 支付时间
    private String orderNo;          // 订单号
    private String tradeNo;          // 交易流水号
    private String status = "FAIL";  // 状态 SUCCESS/FAIL/REFUND
    private String remark;           // 备注
}
