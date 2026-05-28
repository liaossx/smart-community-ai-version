package com.lsx.parking.service;

import com.lsx.parking.entity.ParkingAccount;

import java.math.BigDecimal;

public interface ParkingAccountService {

    ParkingAccount getOrCreateAccount(Long userId);

    void recharge(Long userId, BigDecimal amount);

    void consume(Long userId, BigDecimal amount, Long orderId);
}
