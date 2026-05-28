package com.lsx.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_vehicle")
public class Vehicle {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 车牌鍙凤紙鍞竴锛?*/
    private String plateNo;

    /** 杞︿富用户ID */
    private Long userId;

    private String brand;  // 品牌

    @TableField(exist = false)
    private String color;  // 颜色

    /** ACTIVE / DISABLED */
    private String status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
