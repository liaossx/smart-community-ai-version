package com.lsx.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.RedisLockUtil;
import com.lsx.parking.dto.ParkingGateEnterDTO;
import com.lsx.parking.dto.ParkingGateExitDTO;
import com.lsx.parking.dto.ParkingGateOpenDTO;
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

import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/api/parking/gate")
@Tag(name = "停车-开闸")
public class ParkingGateController {

    @Autowired
    private ParkingGateService parkingGateService;

    @Autowired
    private RedisLockUtil redisLockUtil;


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
    public Result<ParkingGateExitResult> exitGate(
            @RequestBody ParkingGateExitDTO dto) {
        
        String plateNo = dto.getPlateNo();
        if (!StringUtils.hasText(plateNo)) {
            return Result.fail("车牌号不能为空");
        }

        // ==================【Redis 分布式锁控制】=================
        // 将锁移到 Controller 层，确保在开启数据库事务 (@Transactional) 之前先拿锁
        String lockKey = "lock:parking:exit:" + plateNo;
        String lockValue = UUID.randomUUID().toString();
        
        boolean locked = false;
        try {
            // 尝试获取锁：重试 5 次，每次间隔 200ms
            locked = redisLockUtil.tryLockWithRetry(lockKey, lockValue, 10, 5, 200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【出闸中断】车牌 {} 获取锁被中断", plateNo, e);
            return Result.fail("系统繁忙，请稍后重试");
        }

        if (!locked) {
            log.warn("【出闸冲突】车牌 {} 正在处理中，拦截重复请求", plateNo);
            return Result.fail("车辆正在出闸处理中，请勿重复操作");
        }

        try {
            log.info("【出闸请求】开始处理车牌: {}", plateNo);
            ParkingGateExitResult result = parkingGateService.exitGate(dto);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("【出闸异常】车牌 {} 处理失败", plateNo, e);
            return Result.fail("处理失败：" + e.getMessage());
        } finally {
            // 释放锁
            try {
                redisLockUtil.unlock(lockKey, lockValue);
            } catch (Exception e) {
                log.error("【锁释放异常】车牌 {} 释放锁失败", plateNo, e);
            }
        }
    }
}
