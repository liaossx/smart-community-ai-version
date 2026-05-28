package com.lsx.parking.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.parking.dto.ParkingOrderCreateDTO;
import com.lsx.parking.dto.ParkingOrderPayDTO;
import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.entity.ParkingSpaceLease;
import com.lsx.parking.mapper.ParkingOrderMapper;
import com.lsx.parking.mapper.ParkingSpaceLeaseMapper;
import com.lsx.parking.mapper.ParkingSpaceMapper;
import com.lsx.parking.service.ParkingOrderService;
import com.lsx.parking.vo.ParkingOrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
@Service
public class ParkingOrderServiceImpl
        extends ServiceImpl<ParkingOrderMapper, ParkingOrder>
        implements ParkingOrderService {

    private static final String STATUS_UNPAID = "UNPAID";
    private static final String STATUS_PAYING = "PAYING";
    private static final String STATUS_PAID = "PAID";

    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;

    @Autowired
    private ParkingSpaceLeaseMapper parkingSpaceLeaseMapper;

    @Override
    public Long createOrder(ParkingOrderCreateDTO dto) {

        Assert.notNull(dto.getUserId(), "业主ID不能为空");
        Assert.notNull(dto.getAmount(), "订单金额不能为空");

        String orderType;
        Long spaceId = dto.getSpaceId();

        if (spaceId != null) {
            ParkingSpace space = parkingSpaceMapper.selectById(spaceId);
            if (space == null) {
                throw new RuntimeException("车位不存在");
            }
            
            // 只有 TEMP 类型需要校验是否可用，月租/年租通常是在占用状态下续费
            // 但如果是首次开通，也可能是 AVAILABLE
            // 这里逻辑可能需要调整：
            // 1. 如果是临停 (TEMP)，必须是 AVAILABLE
            // 2. 如果是月租/年租 (FIXED)，可以是 AVAILABLE (首次) 或 OCCUPIED/DISABLED (续费)
            
            if ("TEMP".equals(space.getSpaceType())) {
                if (!"AVAILABLE".equals(space.getStatus())) {
                     throw new RuntimeException("车位当前不可用");
                }
                orderType = "TEMP";
            } else {
                // FIXED
                // 允许前端传入 orderType (MONTHLY/YEARLY)，如果不传则默认为 MONTHLY
                orderType = StringUtils.hasText(dto.getOrderType()) ? dto.getOrderType() : "MONTHLY";
            }
            
            dto.setOrderType(orderType);
        } else {
            // 未指定车位 -> 临时订单
            orderType = "TEMP";
            dto.setOrderType(orderType);
        }

        ParkingOrder order = new ParkingOrder();
        BeanUtil.copyProperties(dto, order);

        order.setOrderNo(generateOrderNo());
        order.setStatus(STATUS_UNPAID);
        // 设置社区ID：优先取车位的社区；否则取登录上下文的社区
        if (spaceId != null) {
            ParkingSpace space = parkingSpaceMapper.selectById(spaceId);
            if (space != null) {
                order.setCommunityId(space.getCommunityId());
            }
        }
        if (order.getCommunityId() == null) {
            Long ctxCommunityId = com.lsx.core.common.Util.UserContext.getCommunityId();
            if (ctxCommunityId != null) {
                order.setCommunityId(ctxCommunityId);
            }
        }
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        this.save(order);
        return order.getId();
    }

    @Override
    public IPage<ParkingOrderVO> listMyOrders(Long userId, Integer pageNum, Integer pageSize) {

        Page<ParkingOrder> page = new Page<>(pageNum, pageSize);
        IPage<ParkingOrder> orderPage = this.page(
                page,
                Wrappers.<ParkingOrder>lambdaQuery()
                        .eq(ParkingOrder::getUserId, userId)
                        .orderByDesc(ParkingOrder::getCreateTime)
        );

        return orderPage.convert(this::convertToVO);
    }

    @Override
    public IPage<ParkingOrder> adminListOrders(Integer pageNum, Integer pageSize, String plateNo, String status, String orderType, String startDate, String endDate) {

        Page<ParkingOrder> page = new Page<>(pageNum, pageSize);
        String role = com.lsx.core.common.Util.UserContext.getRole();
        Long currentCommunityId = com.lsx.core.common.Util.UserContext.getCommunityId();
        
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        LambdaQueryWrapper<ParkingOrder> query = Wrappers.<ParkingOrder>lambdaQuery()
                .like(StringUtils.hasText(plateNo), ParkingOrder::getPlateNo, plateNo)
                .eq(StringUtils.hasText(status), ParkingOrder::getStatus, status)
                .eq(StringUtils.hasText(orderType), ParkingOrder::getOrderType, orderType)
                .eq(!"super_admin".equalsIgnoreCase(role) && currentCommunityId != null, ParkingOrder::getCommunityId, currentCommunityId)
                .eq(!"super_admin".equalsIgnoreCase(role) && currentCommunityId == null, ParkingOrder::getId, -1L)
                .orderByDesc(ParkingOrder::getCreateTime);

        if (StringUtils.hasText(startDate)) {
            query.ge(ParkingOrder::getCreateTime, LocalDateTime.parse(startDate + " 00:00:00", df));
        }
        if (StringUtils.hasText(endDate)) {
            query.le(ParkingOrder::getCreateTime, LocalDateTime.parse(endDate + " 23:59:59", df));
        }

        return this.page(page, query);
    }

    @Override
    @Transactional
    public Boolean payOrder(Long orderId, ParkingOrderPayDTO dto) {

        ParkingOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(dto.getUserId())) {
            throw new RuntimeException("无权操作该订单");
        }
        if (!STATUS_UNPAID.equals(order.getStatus())) {
            throw new RuntimeException("订单已处理");
        }

        // 1. 更新订单状态
        order.setStatus(STATUS_PAYING);
        order.setPayChannel(dto.getPayChannel());
        order.setPayRemark(dto.getPayRemark());
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);

        return true;
    }

    private ParkingOrderVO convertToVO(ParkingOrder order) {
        ParkingOrderVO vo = new ParkingOrderVO();
        BeanUtil.copyProperties(order, vo);
        vo.setOrderId(order.getId());
        return vo;
    }

    private String generateOrderNo() {
        return "PK"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + RandomUtil.randomNumbers(4);
    }
}
