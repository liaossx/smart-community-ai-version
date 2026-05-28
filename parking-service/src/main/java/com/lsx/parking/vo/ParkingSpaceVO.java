package com.lsx.parking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParkingSpaceVO {

    @Schema(description = "车位ID")
    private Long id;                 // 车位ID

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "车位编号（旧字段，建议用spaceNo）")
    private String slot;             // 车位编号

    @Schema(description = "车位编号")
    private String spaceNo;

    @Schema(description = "小区名称")
    private String communityName;    // 小区名称

    @Schema(description = "状态 (FREE/OCCUPIED)")
    private String status;

    @Schema(description = "业主姓名")
    private String ownerName;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    /** ===== 租赁信息（来自 lease 表） ===== */
    @Schema(description = "租赁类型")
    private String leaseType;        // MONTHLY / YEARLY / PERPETUAL

    @Schema(description = "租赁开始时间")
    private LocalDateTime leaseStartTime;

    @Schema(description = "租赁结束时间")
    private LocalDateTime leaseEndTime;

    @Schema(description = "租赁状态")
    private String leaseStatus;      // ACTIVE / EXPIRED

    /** ===== 前端展示辅助字段 ===== */
    @Schema(description = "是否可用")
    private Boolean active;           // true = 当前可用

    @Schema(description = "状态文本")
    private String statusText;        // 使用中 / 已过期

    @Schema(description = "车牌号")
    private String plateNo; //车牌号信息
}
