package com.lsx.parking.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ParkingGateExitResult {

    /** 是否允许直接放行 */
    private boolean allowPass;

    /** 是否需要支付 */
    private boolean needPay;

    /** 停车费用（needPay=true 时必有） */
    private BigDecimal amount;

    /** 订单号（needPay=true 时必有） */
    private String orderNo;

    /** 提示信息 */
    private String message;
}
