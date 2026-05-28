package com.lsx.parking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParkingReserveVO {

    @Schema(description = "预订ID")
    private Long reserveId;

    @Schema(description = "车位ID")
    private Long spaceId;

    @Schema(description = "车位编号")
    private String spaceNo;

    @Schema(description = "小区名称")
    private String communityName;

    @Schema(description = "预约状态")
    private String status;

    @Schema(description = "状态文本")
    private String statusText;

    @Schema(description = "预约开始时间")
    private LocalDateTime reserveStartTime;

    @Schema(description = "预约结束时间")
    private LocalDateTime reserveEndTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "用户ID（管理员列表用）")
    private Long userId;

    @Schema(description = "用户姓名（管理员列表用）")
    private String userName;

    @Schema(description = "手机号（管理员列表用）")
    private String phone;
}
