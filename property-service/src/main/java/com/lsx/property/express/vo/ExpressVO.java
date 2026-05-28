package com.lsx.property.express.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快递列表返回
 */
@Data
public class ExpressVO {
    private Long id;
    private String company;
    private String trackingNo;
    private String locationCode;
    private String pickupCode;
    private String status;
    private Boolean authorized;
    private String authorizedName;
    private String authorizedPhone;
    private LocalDateTime authorizeExpireTime;
    private LocalDateTime pickupTime;
    private String remark;
    private LocalDateTime createTime;
}
