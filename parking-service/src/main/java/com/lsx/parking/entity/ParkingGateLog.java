package com.lsx.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_parking_gate_log")
public class ParkingGateLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long spaceId;

    private String plateNo;
    /**
     * FIXED / TEMP
     */
    private String gateType;

    /**
     * OPEN / CLOSE
     */
    private String action;

    /**
     * SUCCESS / FAIL
     */
    private String result;

    private String remark;

    private LocalDateTime createTime;
}
