package com.lsx.property.express.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 授权代取请求
 */
@Data
public class ExpressAuthorizeDTO {

    @Schema(description = "业主ID", required = true)
    private Long userId;

    @Schema(description = "授权人姓名", required = true)
    private String authorizedName;

    @Schema(description = "授权人手机号", required = true)
    private String authorizedPhone;

    @Schema(description = "授权有效期（默认24小时）")
    private LocalDateTime expireTime;
}
