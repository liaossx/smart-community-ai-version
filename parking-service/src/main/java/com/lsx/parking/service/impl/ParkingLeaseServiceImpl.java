package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.parking.dto.ParkingLeaseOrderCreateDTO;
import com.lsx.parking.dto.ParkingLeaseOrderPayDTO;
import com.lsx.parking.entity.ParkingAccount;
import com.lsx.parking.entity.ParkingLeaseOrder;
import com.lsx.parking.entity.ParkingSpaceLease;
import com.lsx.parking.mapper.ParkingAccountMapper;
import com.lsx.parking.mapper.ParkingLeaseOrderMapper;
import com.lsx.parking.mapper.ParkingSpaceLeaseMapper;
import com.lsx.parking.service.ParkingLeaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.mapper.ParkingSpaceMapper;

import com.lsx.parking.entity.ParkingSpacePlate;
import com.lsx.parking.mapper.ParkingPlateMapper;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ParkingLeaseServiceImpl implements ParkingLeaseService {

    @Autowired
    private ParkingLeaseOrderMapper leaseOrderMapper;

    @Autowired
    private ParkingSpaceLeaseMapper leaseMapper;

    @Autowired
    private ParkingAccountMapper accountMapper;

    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;

    @Autowired
    private ParkingPlateMapper plateMapper;
    
    @Override
    public Long createLeaseOrder(ParkingLeaseOrderCreateDTO dto) {
        Assert.notNull(dto.getUserId(), "用户不能为空");
        Assert.notNull(dto.getSpaceId(), "车位不能为空");
        Assert.notNull(dto.getLeaseType(), "租赁类型不能为空");

        BigDecimal amount;
        switch (dto.getLeaseType()) {
            case "MONTHLY": amount = BigDecimal.valueOf(200); break;
            case "YEARLY": amount = BigDecimal.valueOf(2000); break;
            case "PERPETUAL": amount = BigDecimal.valueOf(20000); break;
            default: throw new RuntimeException("非法租赁类型");
        }

        ParkingLeaseOrder order = new ParkingLeaseOrder();
        order.setUserId(dto.getUserId());
        order.setSpaceId(dto.getSpaceId());
        order.setLeaseType(dto.getLeaseType());
        order.setAmount(amount);
        order.setStatus("UNPAID");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        leaseOrderMapper.insert(order);
        
        // 🆕 新增：如果传入了车牌号，且该车位下尚未绑定该车牌，则自动创建绑定记录
        if (StringUtils.hasText(dto.getPlateNo())) {
            // 检查该车位下是否已存在该车牌的 ACTIVE 记录
            ParkingSpacePlate existPlate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                    .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                    .eq(ParkingSpacePlate::getPlateNo, dto.getPlateNo())
                    .eq(ParkingSpacePlate::getStatus, "ACTIVE")
                    .last("LIMIT 1"));
            
            if (existPlate == null) {
                // 如果没有，再检查是否有 PENDING 或 AWAITING_PAYMENT 的记录，如果有则更新，没有则插入
                ParkingSpacePlate pendingPlate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                        .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                        .eq(ParkingSpacePlate::getPlateNo, dto.getPlateNo())
                        .in(ParkingSpacePlate::getStatus, "PENDING", "AWAITING_PAYMENT")
                        .last("LIMIT 1"));
                
                if (pendingPlate != null) {
                    // 已有待审核/待缴费记录，直接将其更新为 ACTIVE (视为管理员办理即通过)
                    // 但通常应该等到支付成功后再 ACTIVE。这里先保持原状态或设为 AWAITING_PAYMENT
                    // 考虑到这是“创建订单”阶段，还未支付。
                    // 策略：如果完全没有记录，则创建一条 status="AWAITING_PAYMENT" 的记录。
                    // 支付成功后，payLeaseOrder 会负责处理 lease，但 plate 的状态也需要在那边同步更新。
                    // 为了简化，这里先创建记录，状态设为 AWAITING_PAYMENT。
                    // 真正的 ACTIVE 应该在支付回调中。
                    // 但 payLeaseOrder 目前只处理 lease。我们需要在 payLeaseOrder 中补充 plate 的处理逻辑。
                    
                    // 这里仅做记录创建/关联，不改 ACTIVE。
                    // 如果记录不存在，创建一条 AWAITING_PAYMENT
                } else {
                    ParkingSpacePlate newPlate = new ParkingSpacePlate();
                    newPlate.setSpaceId(dto.getSpaceId());
                    newPlate.setUserId(dto.getUserId());
                    newPlate.setPlateNo(dto.getPlateNo());
                    newPlate.setStatus("AWAITING_PAYMENT"); // 待支付
                    newPlate.setCreateTime(LocalDateTime.now());
                    newPlate.setUpdateTime(LocalDateTime.now());
                    plateMapper.insert(newPlate);
                    log.info("创建月卡订单时自动创建车牌绑定记录: {}, 车牌: {}", dto.getSpaceId(), dto.getPlateNo());
                }
            }
        }
        
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payLeaseOrder(ParkingLeaseOrderPayDTO dto) {

        ParkingLeaseOrder order = leaseOrderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"UNPAID".equals(order.getStatus())) {
            throw new RuntimeException("订单已支付");
        }
        
        Long userId = order.getUserId();
        String payChannel = dto.getPayChannel();

        // 判断是否需要跳过余额扣除 (CASH/WECHAT/ALIPAY 选线下或外部渠道)
        boolean isSkipBalance = "CASH".equalsIgnoreCase(payChannel)
                || "WECHAT".equalsIgnoreCase(payChannel)
                || "ALIPAY".equalsIgnoreCase(payChannel);

        if (!isSkipBalance) {
            // 1️⃣ 查询账户余额
            ParkingAccount account = accountMapper.selectOne(
                    Wrappers.<ParkingAccount>lambdaQuery()
                            .eq(ParkingAccount::getUserId, userId)
            );
            if (account == null) {
                throw new RuntimeException("账户不存在");
            }

            // 2️⃣ 校验余额
            if (account.getBalance().compareTo(order.getAmount()) < 0) {
                throw new RuntimeException("账户余额不足");
            }

            // 3️⃣ 扣减余额
            account.setBalance(account.getBalance().subtract(order.getAmount()));
            accountMapper.updateById(account);
            log.info("用户 {} 使用余额支付月卡订单 {}", userId, order.getId());
        } else {
            log.info("用户 {} 使用渠道 {} 支付月卡订单 {}，跳过余额扣除", userId, payChannel, order.getId());
        }

        // 4️⃣ 更新订单状态
        order.setStatus("PAID");
        order.setPayChannel(payChannel);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        leaseOrderMapper.updateById(order);

        // 🆕 新增：支付成功后，同步将 AWAITING_PAYMENT 的车牌绑定记录更新为 ACTIVE
        // 尝试根据 spaceId 和 userId 查找最近的 AWAITING_PAYMENT 记录
        ParkingSpacePlate plate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                .eq(ParkingSpacePlate::getSpaceId, order.getSpaceId())
                .eq(ParkingSpacePlate::getUserId, order.getUserId())
                .eq(ParkingSpacePlate::getStatus, "AWAITING_PAYMENT")
                .last("LIMIT 1"));
        
        if (plate != null) {
            plate.setStatus("ACTIVE");
            plate.setUpdateTime(LocalDateTime.now());
            plateMapper.updateById(plate);
            log.info("支付月卡成功，激活车牌绑定: {}", plate.getPlateNo());
        }

        // 🆕 新增：支付成功后，更新车位状态为 OCCUPIED
        if (order.getSpaceId() != null) {
            ParkingSpace space = parkingSpaceMapper.selectById(order.getSpaceId());
            if (space != null) {
                space.setStatus("OCCUPIED");
                space.setUpdateTime(LocalDateTime.now());
                parkingSpaceMapper.updateById(space);
                log.info("车位 {} 状态已更新为 OCCUPIED", order.getSpaceId());
            }
        }

        // 5️⃣ 查询或创建租赁记录
        ParkingSpaceLease lease = leaseMapper.selectOne(
                Wrappers.<ParkingSpaceLease>lambdaQuery()
                        .eq(ParkingSpaceLease::getUserId, userId)
                        .eq(ParkingSpaceLease::getSpaceId, order.getSpaceId())
                        .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                        .last("LIMIT 1")
        );
        
        LocalDateTime now = LocalDateTime.now();
        boolean isNewLease = false;

        if (lease == null) {
            // 如果没找到 active lease，则根据订单信息自动开通
            lease = new ParkingSpaceLease();
            lease.setUserId(userId);
            lease.setSpaceId(order.getSpaceId());
            lease.setLeaseType(order.getLeaseType());
            lease.setStatus("ACTIVE");
            lease.setStartTime(now);
            lease.setSourceOrderId(order.getId());
            lease.setCreateTime(now);
            lease.setUpdateTime(now);
            isNewLease = true;
        } else {
            // 永久车位禁止续费
            if ("PERPETUAL".equals(lease.getLeaseType())) {
                throw new RuntimeException("永久车位无需续费");
            }
        }

        // 6️⃣ 计算有效期起始基准时间
        LocalDateTime baseTime;
        if (isNewLease) {
            baseTime = now;
        } else {
            // 续费场景：若未过期则在原到期时间后追加，若已过期则从现在开始
            baseTime = (lease.getEndTime() != null && lease.getEndTime().isAfter(now))
                    ? lease.getEndTime()
                    : now;
        }

        // 7️⃣ 根据订单类型计算新的结束时间
        LocalDateTime newEnd = null;
        switch (order.getLeaseType()) {
            case "MONTHLY":
                newEnd = baseTime.plusMonths(1);
                break;
            case "YEARLY":
                newEnd = baseTime.plusYears(1);
                break;
            case "PERPETUAL":
                newEnd = null; // 永久车位结束时间为 NULL
                break;
            default:
                throw new RuntimeException("不支持的租赁类型: " + order.getLeaseType());
        }

        lease.setEndTime(newEnd);
        lease.setUpdateTime(now);

        // 8️⃣ 保存或更新租赁记录
        if (isNewLease) {
            leaseMapper.insert(lease);
            log.info("用户 {} 自动开通车位租赁: {}, 类型: {}", userId, order.getSpaceId(), order.getLeaseType());
        } else {
            leaseMapper.updateById(lease);
            log.info("用户 {} 续费车位租赁: {}, 新到期时间: {}", userId, order.getSpaceId(), newEnd);
        }
    }
}
