package com.lsx.property.express.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快递登记实体
 */
@Data
@TableName("biz_express")
public class Express {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;               // 关联业主
    private Long houseId;              // 关联房屋
    private String recipientName;      // 收件人姓名
    private String recipientPhone;     // 收件人电话
    private String company;            // 快递公司
    private String trackingNo;         // 运单号

    private String locationCode;       // 存放位置编码（柜子/货架）
    private String pickupCode;         // 取件码

    private String status;             // WAITING / PICKED
    private Boolean authorized;        // 是否已授权代取
    private String authorizedName;     // 授权人姓名
    private String authorizedPhone;    // 授权人电话
    private LocalDateTime authorizeExpireTime;  // 授权有效期

    private LocalDateTime pickupTime;  // 实际取件时间
    private String remark;             // 备注信息

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
