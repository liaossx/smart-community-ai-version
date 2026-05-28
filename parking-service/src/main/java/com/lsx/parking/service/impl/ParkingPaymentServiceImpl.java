package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.parking.dto.ParkingPaySuccessDTO;
import com.lsx.parking.entity.ParkingGateLog;
import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.mapper.ParkingGateLogMapper;
import com.lsx.parking.mapper.ParkingOrderMapper;
import com.lsx.parking.service.ParkingPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class ParkingPaymentServiceImpl implements ParkingPaymentService {

    @Autowired
    private ParkingOrderMapper parkingOrderMapper;

    @Autowired
    private ParkingGateLogMapper gateLogMapper;

    @Override
    @Transactional
    public void paySuccess(ParkingPaySuccessDTO dto) {

        // 1. 查询订单
        ParkingOrder order = parkingOrderMapper.selectOne(
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getOrderNo, dto.getOrderNo())
                        .last("LIMIT 1")
        );

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if ("PAID".equals(order.getStatus())) {
            return; // 幂等，防止重复回调
        }

        // 2. 更新订单状态
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        order.setPayChannel(dto.getPayChannel());
        order.setUpdateTime(LocalDateTime.now());
        parkingOrderMapper.updateById(order);

        // 3. 写出闸日志（真正放行）
        ParkingGateLog exitLog = new ParkingGateLog();
        exitLog.setPlateNo(order.getPlateNo());
        exitLog.setUserId(order.getUserId());
        exitLog.setSpaceId(order.getSpaceId());
        exitLog.setGateType(order.getOrderType());
        exitLog.setAction("EXIT");
        exitLog.setResult("SUCCESS");
        exitLog.setRemark("支付完成，自动放行");
        exitLog.setCreateTime(LocalDateTime.now());

        gateLogMapper.insert(exitLog);
    }

    @Override
    @Transactional
    public void payCallback(String orderNo, String tradeNo, String status, BigDecimal amount, String payChannel) {
        ParkingOrder order = parkingOrderMapper.selectOne(
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getOrderNo, orderNo)
                        .last("LIMIT 1")
        );
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            if (order.getTradeNo() != null && tradeNo != null && !order.getTradeNo().equals(tradeNo)) {
                throw new RuntimeException("重复回调流水号不一致");
            }
            return;
        }

        if (amount != null && order.getAmount() != null) {
            BigDecimal expected = order.getAmount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal actual = amount.setScale(2, RoundingMode.HALF_UP);
            if (expected.compareTo(actual) != 0) {
                throw new RuntimeException("支付金额不一致");
            }
        }

        if (!"SUCCESS".equalsIgnoreCase(status)) {
            return;
        }

        ParkingOrder update = new ParkingOrder();
        update.setId(order.getId());
        update.setStatus("PAID");
        update.setPayTime(LocalDateTime.now());
        update.setPayChannel(payChannel);
        update.setTradeNo(tradeNo);
        update.setPaidAmount(amount);
        update.setUpdateTime(LocalDateTime.now());
        parkingOrderMapper.updateById(update);

        ParkingGateLog exitLog = new ParkingGateLog();
        exitLog.setPlateNo(order.getPlateNo());
        exitLog.setUserId(order.getUserId());
        exitLog.setSpaceId(order.getSpaceId());
        exitLog.setGateType(order.getOrderType());
        exitLog.setAction("EXIT");
        exitLog.setResult("SUCCESS");
        exitLog.setRemark("支付完成，自动放行");
        exitLog.setCreateTime(LocalDateTime.now());
        gateLogMapper.insert(exitLog);
    }
}
