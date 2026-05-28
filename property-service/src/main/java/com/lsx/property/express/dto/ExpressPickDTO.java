package com.lsx.property.express.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 取件确认请求
 */
@Data
public class ExpressPickDTO {

    @Schema(description = "业主ID", required = true)
    private Long userId;

    @Schema(description = "取件验证码", required = true)
    private String pickupCode;

    @Schema(description = "是否由授权人代取", defaultValue = "false")
    private Boolean byAuthorized;

    @Schema(description = "取件人姓名（授权人必填）")
    private String operatorName;

    @Schema(description = "取件人手机号（授权人必填，用于校验授权信息）")
    private String operatorPhone;

    @Schema(description = "取件备注")
    private String remark;
}
