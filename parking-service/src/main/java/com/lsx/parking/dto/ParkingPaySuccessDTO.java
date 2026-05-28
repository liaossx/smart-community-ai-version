package com.lsx.parking.dto;

import lombok.Data;

@Data
public class ParkingPaySuccessDTO {

    /** 订单号 */
    private String orderNo;

    /** 支付渠道 */
    private String payChannel; // WECHAT / ALIPAY
}
