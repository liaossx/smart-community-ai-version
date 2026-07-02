package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.parking.dto.ParkingGateEnterDTO;
import com.lsx.parking.dto.ParkingGateExitDTO;
import com.lsx.parking.service.ParkingGateService;
import com.lsx.parking.vo.ParkingGateExitResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/parking/gate")
@Tag(name = "停车-开闸")
public class ParkingGateController {

    @Autowired
    private ParkingGateService parkingGateService;

    @PostMapping("/enter")
    @Operation(summary = "车辆入闸", description = "所有扫描车牌或扫码入闸")
    public Result<Void> enterGate(@RequestBody ParkingGateEnterDTO dto) {
        try {
            parkingGateService.enterGate(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("车辆入闸异常", e);
            return Result.fail("入闸失败，请稍后再试");
        }
    }

    @PostMapping("/exit")
    @Operation(summary = "车辆出闸")
    public Result<ParkingGateExitResult> exitGate(@RequestBody ParkingGateExitDTO dto) {

        String plateNo = dto.getPlateNo();
        if (!StringUtils.hasText(plateNo)) {
            return Result.fail("车牌号不能为空");
        }

        try {
            ParkingGateExitResult result = parkingGateService.exitGate(dto);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("【出闸异常】车牌 {} 处理失败", plateNo, e);
            return Result.fail("处理失败: " + e.getMessage());
        }
    }
}
