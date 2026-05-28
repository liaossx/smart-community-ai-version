package com.lsx.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车位淇℃伅
 */
@Data
@TableName("biz_parking_space")
public class ParkingSpace {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long communityId;
    private String communityName;

    /** 车位缂栧彿 */
    private String spaceNo;

    /** TEMP / FIXED */
    private String spaceType;

    /**
     * 状态€侊細AVAILABLE锛堝彲鐢級/ OCCUPIED锛堝凡鍗犵敤锛? RESERVED锛堝凡棰勮锛? DISABLED锛堢鐢級
     */
    private String status;

    /** 閫昏緫删除 */
    private Boolean deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}









