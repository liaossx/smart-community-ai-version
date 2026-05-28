package com.lsx.parking.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.parking.dto.ParkingAuthorizeDTO;
import com.lsx.parking.dto.ParkingSpaceBindDTO;
import com.lsx.parking.dto.ParkingSpaceQueryDTO;
import com.lsx.parking.entity.ParkingAuthorize;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.entity.ParkingSpaceLease;
import com.lsx.parking.entity.ParkingSpacePlate;
import com.lsx.parking.mapper.ParkingAuthorizeMapper;
import com.lsx.parking.mapper.ParkingPlateMapper;
import com.lsx.parking.mapper.ParkingSpaceLeaseMapper;
import com.lsx.parking.mapper.ParkingSpaceMapper;
import com.lsx.parking.service.ParkingSpaceService;
import com.lsx.parking.vo.ParkingAuthorizeVO;
import com.lsx.parking.vo.ParkingSpaceRemainVO;
import com.lsx.parking.vo.ParkingSpaceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.lsx.parking.dto.SpaceRenewDTO;
import com.lsx.parking.dto.SpaceOpenDTO;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

import com.lsx.parking.service.ParkingAccountService;

import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.mapper.ParkingOrderMapper;
import cn.hutool.core.util.RandomUtil;
import java.time.format.DateTimeFormatter;

@Service
@lombok.extern.slf4j.Slf4j
public class ParkingSpaceServiceImpl
        extends ServiceImpl<ParkingSpaceMapper, ParkingSpace>
        implements ParkingSpaceService {

    @Autowired
    private ParkingOrderMapper parkingOrderMapper;


    @Autowired
    private ParkingAccountService parkingAccountService;

    // ... (existing fields)

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(cacheNames = "parkingRemain", allEntries = true),
        @CacheEvict(cacheNames = "mySpaces", key = "#dto.userId")
    })
    public void openSpace(SpaceOpenDTO dto) {
        Assert.notNull(dto.getSpaceId(), "车位ID/记录ID不能为空");
        Assert.notNull(dto.getUserId(), "用户ID不能为空");
        Assert.notNull(dto.getAmount(), "支付金额不能为空");
        Assert.notNull(dto.getDurationMonths(), "购买时长不能为空");

        // 1. 查找待缴费的绑定记录
        // 尝试作为 plateId 查询
        ParkingSpacePlate plate = plateMapper.selectById(dto.getSpaceId());
        
        // 如果按ID没查到，或者查到的记录状态不对/用户不对，尝试按 spaceId + userId 查询
        if (plate == null || !plate.getUserId().equals(dto.getUserId())) {
            plate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                    .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                    .eq(ParkingSpacePlate::getUserId, dto.getUserId())
                    .eq(ParkingSpacePlate::getStatus, "AWAITING_PAYMENT")
                    .last("limit 1"));
        }
        
        if (plate == null) {
             throw new RuntimeException("未找到待缴费的绑定记录");
        }
        
        if (!"AWAITING_PAYMENT".equals(plate.getStatus())) {
             throw new RuntimeException("该记录状态不是待缴费状态");
        }

        // 2. 扣除余额 (biz_parking_account)
        // 使用 plateId 作为关联订单ID（或稍后创建的 leaseId）
        // 这里暂时传 plate.getId()，也可考虑传 0 或其他标识
        try {
            parkingAccountService.consume(dto.getUserId(), dto.getAmount(), plate.getId());
        } catch (Exception e) {
             throw new RuntimeException("余额不足，请充值");
        }
        
        // 3. 更新绑定记录状态
        plate.setStatus("ACTIVE");
        plate.setUpdateTime(LocalDateTime.now());
        plateMapper.updateById(plate);

        ParkingSpace spaceAfterPay = this.getById(plate.getSpaceId());
        if (spaceAfterPay != null) {
            spaceAfterPay.setStatus("OCCUPIED");
            spaceAfterPay.setUpdateTime(LocalDateTime.now());
            this.updateById(spaceAfterPay);
        }
        
        // 4. 更新/创建租赁记录
        ParkingSpaceLease lease = leaseMapper.selectOne(Wrappers.<ParkingSpaceLease>lambdaQuery()
                .eq(ParkingSpaceLease::getSpaceId, plate.getSpaceId())
                .eq(ParkingSpaceLease::getUserId, dto.getUserId())
                .last("limit 1"));
        
        LocalDateTime now = LocalDateTime.now();
        if (lease == null) {
            lease = new ParkingSpaceLease();
            lease.setSpaceId(plate.getSpaceId());
            lease.setUserId(dto.getUserId());
            lease.setLeaseType("MONTHLY"); // 默认为月租
            lease.setStartTime(now);
            lease.setCreateTime(now);
        }
        
        // 更新有效期
        lease.setStartTime(now);
        lease.setEndTime(now.plusMonths(dto.getDurationMonths()));
        lease.setStatus("ACTIVE");
        lease.setUpdateTime(now);
        
        if (lease.getId() == null) {
            leaseMapper.insert(lease);
        } else {
            leaseMapper.updateById(lease);
        }

        // 5. 插入统一订单记录 (ParkingOrder)
        ParkingOrder order = new ParkingOrder();
        order.setOrderNo("PK" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + RandomUtil.randomNumbers(4));
        order.setUserId(dto.getUserId());
        order.setSpaceId(plate.getSpaceId());
        order.setPlateNo(plate.getPlateNo());
        order.setOrderType("MONTHLY"); // 或根据时长判断 YEARLY
        if (dto.getDurationMonths() >= 12) {
             order.setOrderType("YEARLY");
        }
        order.setAmount(dto.getAmount());
        order.setStatus("PAID");
        order.setPayTime(now);
        order.setPayChannel(dto.getPayMethod());
        order.setPayRemark("首次开通车位: " + plate.getPlateNo() + ", 时长: " + dto.getDurationMonths() + "个月");
        
        // 获取社区ID
        ParkingSpace space = this.getById(plate.getSpaceId());
        if (space != null) {
            order.setCommunityId(space.getCommunityId());
        }
        
        order.setStartTime(now);
        order.setEndTime(lease.getEndTime());
        order.setCreateTime(now);
        order.setUpdateTime(now);
        
        parkingOrderMapper.insert(order);

        // 6. 调用道闸系统接口 (模拟)
        // log.info("调用道闸系统下发车牌权限: {}", plate.getPlateNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "mySpaces", key = "#dto.userId")
    public void renewSpace(SpaceRenewDTO dto) {
        Assert.notNull(dto.getSpaceId(), "车位ID不能为空");
        Assert.notNull(dto.getDurationMonths(), "续费时长不能为空");
        Assert.notNull(dto.getAmount(), "支付金额不能为空");
        Assert.notNull(dto.getUserId(), "用户ID不能为空");

        // 1. 校验车位和租赁关系
        ParkingSpaceLease lease = leaseMapper.selectOne(Wrappers.<ParkingSpaceLease>lambdaQuery()
                .eq(ParkingSpaceLease::getSpaceId, dto.getSpaceId())
                .eq(ParkingSpaceLease::getUserId, dto.getUserId())
                .last("limit 1"));

        if (lease == null) {
            // 如果是首次租赁（没有历史记录），可能需要新建 Lease
            // 这里假设必须先有租赁记录（即使过期的）才能续费
            // 如果要支持新购，逻辑会不同
             throw new RuntimeException("未找到该车位的租赁记录，请联系管理员开通");
        }

        // 2. 扣除余额 (biz_parking_account)
        // 使用 lease.getId() 作为关联ID
        try {
            parkingAccountService.consume(dto.getUserId(), dto.getAmount(), lease.getId());
        } catch (Exception e) {
             throw new RuntimeException("余额不足，请充值");
        }

        // 3. 更新租赁时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newEndTime;
        
        // 获取当前结束时间，如果为 null 则视为已过期
        LocalDateTime currentEndTime = lease.getEndTime();
        
        if (currentEndTime == null || currentEndTime.isBefore(now)) {
            // 已过期或无截止时间，从现在开始计算
            lease.setStartTime(now); // 重新激活
            newEndTime = now.plusMonths(dto.getDurationMonths());
        } else {
            // 未过期，续在后面
            newEndTime = currentEndTime.plusMonths(dto.getDurationMonths());
        }
        
        lease.setEndTime(newEndTime);
        lease.setStatus("ACTIVE");
        lease.setUpdateTime(now);
        
        leaseMapper.updateById(lease);
        
        // 4. 同步更新车位状态（如果之前是 DISABLED 或其他状态）
        ParkingSpace space = this.getById(dto.getSpaceId());
        if (space != null) {
            // 固定车位被租赁后，通常状态设为 DISABLED (占用)
            if (!"DISABLED".equals(space.getStatus())) {
                space.setStatus("DISABLED");
                space.setUpdateTime(now);
                this.updateById(space);
            }
        }
        
        // 5. 插入统一订单记录 (ParkingOrder)
        ParkingOrder order = new ParkingOrder();
        order.setOrderNo("PK" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + RandomUtil.randomNumbers(4));
        order.setUserId(dto.getUserId());
        order.setSpaceId(dto.getSpaceId());
        
        // 尝试获取车牌号
        String plateNo = "";
        ParkingSpacePlate plate = plateMapper.selectOne(Wrappers.<ParkingSpacePlate>lambdaQuery()
                .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                .eq(ParkingSpacePlate::getUserId, dto.getUserId())
                .last("limit 1"));
        if (plate != null) {
            plateNo = plate.getPlateNo();
        }
        order.setPlateNo(plateNo);
        
        order.setOrderType("MONTHLY"); // 或根据时长判断 YEARLY
        if (dto.getDurationMonths() >= 12) {
             order.setOrderType("YEARLY");
        }
        order.setAmount(dto.getAmount());
        order.setStatus("PAID");
        order.setPayTime(now);
        order.setPayChannel("BALANCE"); // 续费通常也是余额
        order.setPayRemark("车位续费: " + plateNo + ", 时长: " + dto.getDurationMonths() + "个月");
        
        if (space != null) {
            order.setCommunityId(space.getCommunityId());
        }
        
        // 续费的起始时间应该是之前的结束时间？或者是现在？
        // 这里简单处理为本次缴费覆盖的时间段
        // 注意：renewSpace 方法中已经计算了 newEnd，但这里我们无法直接获取之前的 end
        // 为了简化，startTime 设为 now (如果不严谨)，或者设为 lease.getStartTime() (也不对)
        // 比较严谨的是：如果是续期，startTime 应该是之前的 endTime。
        // 但由于 newEndTime 已经更新到 lease 对象里了，我们无法轻易得知“这一笔”增加的时间段
        // 暂且将 startTime 设为 now，endTime 设为 newEndTime
        order.setStartTime(now); 
        order.setEndTime(newEndTime);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        
        parkingOrderMapper.insert(order);
    }

    // ... (existing methods)

    private static final String SPACE_AVAILABLE = "AVAILABLE";
    private static final String SPACE_FIXED = "FIXED";
    private static final List<String> BINDABLE_SPACE_STATUS =
            java.util.Arrays.asList(SPACE_AVAILABLE, "FREE", "IDLE", "UNUSED", "0");

    @Autowired
    private ParkingSpaceLeaseMapper leaseMapper;

    @Autowired
    private ParkingAuthorizeMapper parkingAuthorizeMapper;

    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;

    @Autowired
    private ParkingPlateMapper plateMapper;

    /**
     * 查询剩余车位
     */
    @Override
    @Cacheable(cacheNames = "parkingRemain", key = "#communityId != null ? #communityId : 'all'")
    public ParkingSpaceRemainVO getRemaining(Long communityId) {

        long tempRemain = this.count(Wrappers.<ParkingSpace>lambdaQuery()
                .eq(communityId != null, ParkingSpace::getCommunityId, communityId)
                .eq(ParkingSpace::getSpaceType, "TEMP")
                .eq(ParkingSpace::getStatus, SPACE_AVAILABLE));

        long fixedRemain = this.count(Wrappers.<ParkingSpace>lambdaQuery()
                .eq(communityId != null, ParkingSpace::getCommunityId, communityId)
                .eq(ParkingSpace::getSpaceType, SPACE_FIXED)
                .eq(ParkingSpace::getStatus, SPACE_AVAILABLE));

        ParkingSpaceRemainVO vo = new ParkingSpaceRemainVO();
        vo.setCommunityId(communityId);
        vo.setTempRemaining(tempRemain);
        vo.setFixedRemaining(fixedRemain);

        if (communityId != null) {
            ParkingSpace space = this.getOne(
                    Wrappers.<ParkingSpace>lambdaQuery()
                            .eq(ParkingSpace::getCommunityId, communityId)
                            .last("limit 1")
            );
            if (space != null) {
                vo.setCommunityName(space.getCommunityName());
            }
        }
        return vo;
    }

    /**
     * 绑定固定车位（只做“占位”，不产生使用权）
     */
    @Override
    @CacheEvict(cacheNames = "parkingRemain", allEntries = true)
    public Boolean bindSpace(ParkingSpaceBindDTO dto) {

        Assert.notNull(dto.getSpaceId(), "车位ID不能为空");

        ParkingSpace space = this.getById(dto.getSpaceId());
        if (space == null) {
            throw new RuntimeException("车位不存在");
        }
        if (!SPACE_AVAILABLE.equals(space.getStatus())) {
            throw new RuntimeException("车位不可绑定");
        }
        if (!SPACE_FIXED.equals(space.getSpaceType())) {
            throw new RuntimeException("仅固定车位可绑定");
        }

        // 仅改变车位状态
        space.setStatus("DISABLED");
        space.setUpdateTime(LocalDateTime.now());

        return this.updateById(space);
    }

    /**
     * 查询我拥有的车位（基于使用权）
     */
    @Override
    @Cacheable(cacheNames = "mySpaces", key = "#userId")
    public List<ParkingSpaceVO> listMySpaces(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");

        // 查询用户相关的车辆绑定记录 (PENDING, AWAITING_PAYMENT, ACTIVE)
        List<ParkingSpacePlate> plates = plateMapper.selectList(
                Wrappers.<ParkingSpacePlate>lambdaQuery()
                        .eq(ParkingSpacePlate::getUserId, userId)
                        .in(ParkingSpacePlate::getStatus, "PENDING", "AWAITING_PAYMENT", "ACTIVE")
                        .orderByDesc(ParkingSpacePlate::getCreateTime)
        );

        LocalDateTime now = LocalDateTime.now();

        return plates.stream().map(plate -> {
            ParkingSpace space = parkingSpaceMapper.selectById(plate.getSpaceId());
            if (space == null) return null;

            ParkingSpaceVO vo = new ParkingSpaceVO();
            vo.setId(space.getId());
            vo.setSlot(space.getSpaceNo());
            vo.setSpaceNo(space.getSpaceNo());
            vo.setCommunityName(space.getCommunityName());
            vo.setPlateNo(plate.getPlateNo());

            // 状态判断
            String plateStatus = plate.getStatus();
            
            // 尝试查找 Lease 信息
            ParkingSpaceLease lease = leaseMapper.selectOne(Wrappers.<ParkingSpaceLease>lambdaQuery()
                    .eq(ParkingSpaceLease::getSpaceId, plate.getSpaceId())
                    .eq(ParkingSpaceLease::getUserId, userId)
                    .last("limit 1"));

            if (lease != null) {
                vo.setLeaseType(lease.getLeaseType());
                vo.setLeaseStartTime(lease.getStartTime());
                vo.setLeaseEndTime(lease.getEndTime());
                
                // 如果是 ACTIVE 状态，进一步检查时间是否过期
                if ("ACTIVE".equals(plateStatus)) {
                    boolean isLeaseActive = lease.getEndTime() != null && lease.getEndTime().isAfter(now);
                    vo.setLeaseStatus(isLeaseActive ? "ACTIVE" : "EXPIRED");
                    vo.setActive(isLeaseActive);
                    vo.setStatusText(isLeaseActive ? "使用中" : "已过期");
                } else {
                    // 非 ACTIVE 状态直接使用 Plate 状态
                    vo.setLeaseStatus(plateStatus);
                    vo.setActive(false);
                    if ("PENDING".equals(plateStatus)) vo.setStatusText("审核中");
                    else if ("AWAITING_PAYMENT".equals(plateStatus)) vo.setStatusText("待缴费");
                }
            } else {
                // 无 Lease 记录
                vo.setLeaseStatus(plateStatus);
                vo.setActive(false);
                if ("PENDING".equals(plateStatus)) vo.setStatusText("审核中");
                else if ("AWAITING_PAYMENT".equals(plateStatus)) vo.setStatusText("待缴费");
                else if ("ACTIVE".equals(plateStatus)) vo.setStatusText("使用中(无租赁记录)");
            }

            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    /**
     * 固定车位授权访客
     */
    @Override
    public Boolean authorizeSpace(Long spaceId, ParkingAuthorizeDTO dto) {

        Assert.notNull(spaceId, "车位ID不能为空");
        Assert.notNull(dto.getUserId(), "用户ID不能为空");

        // 校验是否有有效使用权
        ParkingSpaceLease lease = leaseMapper.selectOne(
                Wrappers.<ParkingSpaceLease>lambdaQuery()
                        .eq(ParkingSpaceLease::getSpaceId, spaceId)
                        .eq(ParkingSpaceLease::getUserId, dto.getUserId())
                        .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                        .last("limit 1")
        );

        if (lease == null) {
            throw new RuntimeException("无该车位的有效使用权");
        }

        ParkingAuthorize authorize = new ParkingAuthorize();
        authorize.setSpaceId(spaceId);
        authorize.setUserId(dto.getUserId());
        authorize.setAuthorizedName(dto.getAuthorizedName());
        authorize.setAuthorizedPhone(dto.getAuthorizedPhone());
        authorize.setPlateNo(dto.getPlateNo());
        authorize.setStartTime(LocalDateTime.now());
        authorize.setEndTime(dto.getEndTime());
        authorize.setStatus("ACTIVE");
        authorize.setCreateTime(LocalDateTime.now());
        authorize.setUpdateTime(LocalDateTime.now());

        parkingAuthorizeMapper.insert(authorize);
        return true;
    }

    /**
     * 查询我的授权记录
     */
    @Override
    public IPage<ParkingAuthorizeVO> listMyAuthorizes(
            Long userId, Integer pageNum, Integer pageSize) {

        Page<ParkingAuthorize> page = new Page<>(pageNum, pageSize);

        IPage<ParkingAuthorize> authorizePage =
                parkingAuthorizeMapper.selectPage(page,
                        Wrappers.<ParkingAuthorize>lambdaQuery()
                                .eq(ParkingAuthorize::getUserId, userId)
                                .orderByDesc(ParkingAuthorize::getCreateTime));

        return authorizePage.convert(record -> {
            ParkingAuthorizeVO vo = new ParkingAuthorizeVO();
            BeanUtil.copyProperties(record, vo);

            ParkingSpace space = this.getById(record.getSpaceId());
            if (space != null) {
                vo.setSpaceNo(space.getSpaceNo());
            }

            if (record.getEndTime() != null &&
                    record.getEndTime().isBefore(LocalDateTime.now())) {
                vo.setStatus("EXPIRED");
            } else {
                vo.setStatus(record.getStatus());
            }
            return vo;
        });
    }

    @Override
    public IPage<ParkingSpaceVO> adminListSpaces(ParkingSpaceQueryDTO dto) {
        String role = com.lsx.core.common.Util.UserContext.getRole();
        Long currentCommunityId = com.lsx.core.common.Util.UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId == null) {
                // 无社区则查不到任何数据
                dto.setCommunityId(-1L);
            } else {
                // 强制限定为当前社区
                dto.setCommunityId(currentCommunityId);
            }
        }
        Page<ParkingSpaceVO> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        return parkingSpaceMapper.selectAdminPage(page, dto);
    }

    @Override
    public IPage<ParkingSpaceVO> listAvailableFixedSpaces(Long communityId, Integer pageNum, Integer pageSize) {
        Page<ParkingSpace> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<ParkingSpace> query = Wrappers.lambdaQuery();
        query.eq(ParkingSpace::getSpaceType, SPACE_FIXED)
             .eq(ParkingSpace::getDeleted, false)
             .and(wrapper -> wrapper
                     .in(ParkingSpace::getStatus, BINDABLE_SPACE_STATUS)
                     .or()
                     .isNull(ParkingSpace::getStatus)
                     .or()
                     .eq(ParkingSpace::getStatus, ""))
             .apply("NOT EXISTS (SELECT 1 FROM biz_parking_space_plate p " +
                     "WHERE p.space_id = biz_parking_space.id " +
                     "AND p.status IN ('PENDING','AWAITING_PAYMENT','ACTIVE'))")
             .apply("NOT EXISTS (SELECT 1 FROM biz_parking_space_lease l " +
                     "WHERE l.space_id = biz_parking_space.id " +
                     "AND l.status = 'ACTIVE')")
             .orderByAsc(ParkingSpace::getSpaceNo);
             
        if (communityId != null) {
            query.eq(ParkingSpace::getCommunityId, communityId);
        }

        log.info("查询可供车辆绑定的固定车位: communityId={}, pageNum={}, pageSize={}, statusIn={}, sqlSegment={}",
                communityId, pageNum, pageSize, BINDABLE_SPACE_STATUS, query.getSqlSegment());
        
        IPage<ParkingSpace> spacePage = parkingSpaceMapper.selectPage(page, query);
        
        return spacePage.convert(space -> {
            ParkingSpaceVO vo = new ParkingSpaceVO();
            BeanUtil.copyProperties(space, vo);
            if (isBindableSpaceStatus(space.getStatus())) {
                vo.setStatus(SPACE_AVAILABLE);
            }
            vo.setSlot(space.getSpaceNo());
            return vo;
        });
    }

    private boolean isBindableSpaceStatus(String status) {
        return status == null || status.trim().isEmpty() || BINDABLE_SPACE_STATUS.contains(status);
    }
}
