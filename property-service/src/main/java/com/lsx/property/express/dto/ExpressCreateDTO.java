package com.lsx.property.express.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 物业登记快递请求
 */
@Data
public class ExpressCreateDTO {

    @Schema(description = "收件业主ID", required = true)
    private Long userId;

    @Schema(description = "收件人姓名", required = true)
    private String recipientName;

    @Schema(description = "联系电话", required = true)
    private String recipientPhone;

    @Schema(description = "关联房屋ID", required = true)
    private Long houseId;

    @Schema(description = "快递公司", required = true)
    private String company;

    @Schema(description = "运单号", required = true)
    private String trackingNo;

    @Schema(description = "备注信息")
    private String remark;
}
