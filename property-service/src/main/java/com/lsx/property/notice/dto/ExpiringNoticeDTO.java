package com.lsx.property.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "即将过期通知DTO")
public class ExpiringNoticeDTO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "目标类型")
    private String targetType;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "剩余天数")
    private Long daysLeft;

    @Schema(description = "置顶状态")
    private Boolean topFlag;
}
