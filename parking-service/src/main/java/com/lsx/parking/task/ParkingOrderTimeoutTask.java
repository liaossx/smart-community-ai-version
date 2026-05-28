package com.lsx.parking.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.mapper.ParkingOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingOrderTimeoutTask {

    private final ParkingOrderMapper parkingOrderMapper;

    private static final long TIMEOUT_MINUTES = 15;

    // TODO: 为了演示“改造前”数据，临时关闭定时任务
    // @Scheduled(cron = "0 * * * * ?")
    public void cancelUnpaidOrders() {

        log.info("【定时任务】开始扫描超时未支付订单");

        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        // 1. 查询候选订单（只查，不直接改）
        List<ParkingOrder> overdueOrders = parkingOrderMapper.selectList(
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getStatus, "UNPAID")
                        .lt(ParkingOrder::getCreateTime, timeoutThreshold)
                        .last("LIMIT 100")
        );

        log.info("【定时任务】发现 {} 条超时订单", overdueOrders.size());

        for (ParkingOrder order : overdueOrders) {
            try {
                // 2. 条件更新（核心！）
                int rows = parkingOrderMapper.update(
                        null,
                        Wrappers.<ParkingOrder>lambdaUpdate()
                                .eq(ParkingOrder::getId, order.getId())
                                .eq(ParkingOrder::getStatus, "UNPAID")
                                .set(ParkingOrder::getStatus, "CANCEL")
                                .set(ParkingOrder::getUpdateTime, LocalDateTime.now())
                );

                if (rows > 0) {
                    log.info("【订单取消成功】orderNo={}", order.getOrderNo());
                } else {
                    log.info("【订单已被处理】orderNo={}，可能已支付或已取消", order.getOrderNo());
                }

            } catch (Exception e) {
                log.error("【订单取消异常】orderNo={}", order.getOrderNo(), e);
            }
        }

        log.info("【定时任务】扫描完成");
    }
}
