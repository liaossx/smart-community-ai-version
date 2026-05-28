package com.lsx.parking.service;

import com.lsx.parking.dto.ParkingPaySuccessDTO;

import java.math.BigDecimal;

public interface ParkingPaymentService {

    void paySuccess(ParkingPaySuccessDTO dto);

    void payCallback(String orderNo, String tradeNo, String status, BigDecimal amount, String payChannel);
}
