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

/**
 * 停车订单定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingOrderTask {

    private final ParkingOrderMapper parkingOrderMapper;

    /**
     * 每隔 5 分钟清理一次超时未支付订单
     * 超时定义：创建超过 30 分钟仍未支付的临时订单
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void cleanTimeoutUnpaidOrders() {
        log.info("【定时任务】开始扫描超时未支付订单...");
        
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(30);
        
        // 查询超时订单
        List<ParkingOrder> timeoutOrders = parkingOrderMapper.selectList(
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getStatus, "UNPAID")
                        .eq(ParkingOrder::getOrderType, "TEMP")
                        .lt(ParkingOrder::getCreateTime, timeoutTime)
        );

        if (timeoutOrders.isEmpty()) {
            log.info("【定时任务】未发现超时未支付订单");
            return;
        }

        log.info("【定时任务】发现 {} 个超时订单，准备清理", timeoutOrders.size());

        for (ParkingOrder order : timeoutOrders) {
            try {
                order.setStatus("CANCELLED");
                order.setUpdateTime(LocalDateTime.now());
                parkingOrderMapper.updateById(order);
                log.info("【定时任务】订单 {} 已自动取消", order.getOrderNo());
            } catch (Exception e) {
                log.error("【定时任务】清理订单 {} 失败", order.getOrderNo(), e);
            }
        }
        
        log.info("【定时任务】超时订单清理完成");
    }
}
