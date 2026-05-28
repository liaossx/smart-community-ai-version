package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.parking.dto.ParkingLeaseOrderCreateDTO;
import com.lsx.parking.dto.ParkingLeaseOrderPayDTO;
import com.lsx.parking.service.ParkingLeaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/parking/lease")
@Tag(name = "停车-月卡")
public class ParkingLeaseController {

    @Autowired
    private ParkingLeaseService parkingLeaseService;

    /**
     * 购买/续费月卡，生成订单
     */
    @PostMapping("/order/create")
    @Operation(summary = "创建月卡订单", description = "购买或续费月卡，生成未支付订单")
    public Result<Long> createLeaseOrder(@RequestBody ParkingLeaseOrderCreateDTO dto) {
        try {
            Long orderId = parkingLeaseService.createLeaseOrder(dto);
            return Result.success(orderId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建月卡订单异常", e);
            return Result.fail("创建月卡订单失败，请稍后重试");
        }
    }

    /**
     * 支付月卡订单
     */
    @PostMapping("/order/pay")
    @Operation(summary = "支付月卡订单", description = "支付月卡订单后系统自动生效或续期")
    public Result<Void> payLeaseOrder(@RequestBody ParkingLeaseOrderPayDTO dto) {
        try {
            log.info("接收到月卡支付请求: 订单ID={}, 支付渠道={}", dto.getOrderId(), dto.getPayChannel());
            parkingLeaseService.payLeaseOrder(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("支付月卡订单异常", e);
            return Result.fail("支付月卡订单失败，请稍后重试");
        }
    }
}
