package com.lsx.property.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "批量通知过期DTO")
public class BatchNoticeExpireDTO {

    @Schema(description = "通知ID列表", required = true)
    @NotEmpty(message = "通知ID列表不能为空")
    private List<Long> noticeIds;

    @Schema(description = "过期类型", required = true)
    @NotNull(message = "过期类型不能为空")
    private NoticeExpireDTO.ExpireType expireType;

    @Schema(description = "自定义过期时间")
    private LocalDateTime customExpireTime;

    @Schema(description = "天数")
    private Integer days;
}
