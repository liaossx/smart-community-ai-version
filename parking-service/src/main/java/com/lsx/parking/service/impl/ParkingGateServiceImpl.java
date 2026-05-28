package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.core.common.Util.RedisLockUtil;
import com.lsx.parking.dto.ParkingGateEnterDTO;
import com.lsx.parking.dto.ParkingGateExitDTO;
import com.lsx.parking.entity.*;
import com.lsx.parking.mapper.*;
import com.lsx.parking.service.ParkingGateService;
import com.lsx.parking.vo.ParkingGateExitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingGateServiceImpl implements ParkingGateService {

    private final ParkingSpaceMapper parkingSpaceMapper;
    private final ParkingOrderMapper parkingOrderMapper;
    private final ParkingGateLogMapper gateLogMapper;
    private final ParkingSpaceLeaseMapper leaseMapper;
    private final VehicleMapper vehicleMapper;
    private final RedisLockUtil redisLockUtil;

    @Autowired
    private DataSource dataSource;

    @Override
    @Transactional
    public void enterGate(ParkingGateEnterDTO dto) {

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            log.info("数据库URL: {}", metaData.getURL());
            log.info("当前数据库: {}", conn.getCatalog());
        } catch (Exception e) {
            log.error("获取数据库连接信息失败", e);
        }
        if (!StringUtils.hasText(dto.getPlateNo())) {
            throw new RuntimeException("车牌号不能为空");
        }

        String plateNo = dto.getPlateNo();

        // 1. 根据车牌查车辆
        Vehicle vehicle = vehicleMapper.selectByPlateNo(plateNo);

        boolean isOwnerVehicle = vehicle != null;
        ParkingSpaceLease lease = null;

        if (isOwnerVehicle) {
            // 2. 查是否有有效车位使用权（包月 / 固定）
            lease = leaseMapper.selectActiveLeaseByUser(vehicle.getUserId());
        }

        // 3. 记录入闸日志
        ParkingGateLog gateLog = new ParkingGateLog();
        gateLog.setUserId(isOwnerVehicle ? vehicle.getUserId() : null);
        gateLog.setSpaceId(lease != null ? lease.getSpaceId() : null);
        gateLog.setGateType(isOwnerVehicle ? "OWNER" : "TEMP");
        gateLog.setAction("ENTER");
        gateLog.setResult("SUCCESS");
        gateLog.setPlateNo(plateNo);
        
        if (!isOwnerVehicle) {
            gateLog.setRemark("外来车辆入闸");
        } else if (lease != null) {
            gateLog.setRemark("业主固定车位入闸");
        } else {
            gateLog.setRemark("业主临时停车入闸");
        }

        gateLog.setCreateTime(LocalDateTime.now());
        gateLogMapper.insert(gateLog);
    }

    @Override
    @Transactional
    public ParkingGateExitResult exitGate(ParkingGateExitDTO dto) {
        String plateNo = dto.getPlateNo();
        LocalDateTime exitTime = LocalDateTime.now();

        // ==================【1️⃣：基础参数校验】=================
        if (!StringUtils.hasText(plateNo)) {
            throw new RuntimeException("车牌号不能为空");
        }

        log.info("【出闸业务处理】车牌: {}", plateNo);

        // ==================【3️⃣：查询最近一次入闸记录】=================
        ParkingGateLog enterLog = gateLogMapper.selectOne(
                Wrappers.<ParkingGateLog>lambdaQuery()
                        .eq(ParkingGateLog::getPlateNo, plateNo)
                        .eq(ParkingGateLog::getAction, "ENTER")
                        .orderByDesc(ParkingGateLog::getCreateTime)
                        .last("LIMIT 1")
        );

        if (enterLog == null) {
            log.warn("【出闸失败】车牌 {} 未找到入闸记录", plateNo);
            throw new RuntimeException("未找到入闸记录，无法出闸");
        }

        LocalDateTime enterTime = enterLog.getCreateTime();

        // ==================【4️⃣：固定车位校验】=================
        ParkingSpaceLease lease = null;
        if (enterLog.getUserId() != null) {
            lease = leaseMapper.selectOne(
                    Wrappers.<ParkingSpaceLease>lambdaQuery()
                            .eq(ParkingSpaceLease::getUserId, enterLog.getUserId())
                            .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                            .last("LIMIT 1")
            );
        }

        boolean isFixedOwner = lease != null &&
                (lease.getEndTime() == null || lease.getEndTime().isAfter(exitTime));

        // ==================【5️⃣：固定车位 -> 直接放行】=================
        if (isFixedOwner) {
            log.info("【固定车位放行】车牌 {} 为固定车位业主，直接放行", plateNo);

            ParkingGateLog exitLog = new ParkingGateLog();
            exitLog.setPlateNo(plateNo);
            exitLog.setUserId(enterLog.getUserId());
            exitLog.setSpaceId(lease.getSpaceId());
            exitLog.setGateType("FIXED");
            exitLog.setAction("EXIT");
            exitLog.setResult("SUCCESS");
            exitLog.setRemark("固定车位业主直接放行");
            exitLog.setCreateTime(exitTime);
            gateLogMapper.insert(exitLog);

            ParkingGateExitResult result = new ParkingGateExitResult();
            result.setAllowPass(true);
            result.setNeedPay(false);
            result.setMessage("固定车位业主，直接放行");
            return result;
        }

        // ==================【6️⃣：数据库层防重复订单（第二道防线）】=================
        ParkingOrder existOrder = parkingOrderMapper.selectOne(
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getPlateNo, plateNo)
                        .in(ParkingOrder::getStatus, "UNPAID", "PAID")
                        .orderByDesc(ParkingOrder::getCreateTime)
                        .last("LIMIT 1")
        );

        if (existOrder != null) {
            log.warn("【出闸拦截】车牌 {} 存在未完成订单 orderNo={}",
                    plateNo, existOrder.getOrderNo());

            ParkingGateExitResult result = new ParkingGateExitResult();
            result.setAllowPass(false);
            result.setNeedPay(true);
            result.setAmount(existOrder.getAmount());
            result.setOrderNo(existOrder.getOrderNo());
            result.setMessage("存在未完成订单，请先支付");
            return result;
        }

        // ==================【7️⃣：临时车计费逻辑】=================
        long minutes = Duration.between(enterTime, exitTime).toMinutes();
        long hours = minutes <= 0 ? 1 : (minutes + 59) / 60;
        BigDecimal amount = BigDecimal.valueOf(hours * 10);

        log.info("【计费信息】车牌 {} 停车时长 {} 分钟，计费 {} 元",
                plateNo, minutes, amount);

        // ==================【8️⃣：生成唯一停车订单】=================
        String orderNo = generateOrderNo();
        ParkingOrder order = new ParkingOrder();
        order.setOrderNo(orderNo);
        order.setPlateNo(plateNo);
        order.setUserId(enterLog.getUserId() == null ? 0L : enterLog.getUserId());

        if (enterLog.getSpaceId() != null) {
            order.setSpaceId(enterLog.getSpaceId());
        } else {
            order.setSpaceId(0L);
        }

        order.setOrderType("TEMP");
        order.setAmount(amount);
        order.setStatus("UNPAID");
        order.setStartTime(enterTime);
        order.setEndTime(exitTime);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        parkingOrderMapper.insert(order);

        log.info("【订单生成】车牌 {} 生成停车订单 orderNo={}, amount={}",
                plateNo, orderNo, amount);

        // ==================【9️⃣：返回支付信息（临时车不放行）】=================
        ParkingGateExitResult result = new ParkingGateExitResult();
        result.setAllowPass(false);
        result.setNeedPay(true);
        result.setAmount(amount);
        result.setOrderNo(orderNo);
        result.setMessage("请支付停车费后出闸");

        return result;
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return "P" + timePart + randomPart;
    }

    private void recordExitLog(String plateNo,
                               ParkingGateLog enterLog,
                               String gateType,
                               String remark) {

        ParkingGateLog exitLog = new ParkingGateLog();
        exitLog.setPlateNo(plateNo);
        exitLog.setUserId(enterLog.getUserId());
        exitLog.setSpaceId(enterLog.getSpaceId());
        exitLog.setGateType(gateType);
        exitLog.setAction("EXIT");
        exitLog.setResult("SUCCESS");
        exitLog.setRemark(remark);
        exitLog.setCreateTime(LocalDateTime.now());

        gateLogMapper.insert(exitLog);
    }
}
