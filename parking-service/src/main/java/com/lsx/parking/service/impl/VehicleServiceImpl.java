package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.parking.dto.ParkingCarAuditDTO;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.entity.ParkingSpacePlate;
import com.lsx.parking.mapper.ParkingPlateMapper;
import com.lsx.parking.mapper.ParkingSpaceMapper;
import com.lsx.parking.vo.ParkingCarAuditVO;
import com.lsx.parking.client.UserServiceClient;
import com.lsx.parking.dto.external.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.parking.dto.VehicleBindDTO;
import com.lsx.parking.entity.Vehicle;
import com.lsx.parking.mapper.VehicleMapper;
import com.lsx.parking.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class VehicleServiceImpl extends ServiceImpl<VehicleMapper, Vehicle> implements VehicleService {

    private static final List<String> BINDABLE_SPACE_STATUS =
            Arrays.asList("AVAILABLE", "FREE", "IDLE", "UNUSED", "0");

    @Autowired
    private ParkingPlateMapper plateMapper;
    @Autowired
    private ParkingSpaceMapper spaceMapper;
    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    @Transactional
    public void bindVehicle(VehicleBindDTO dto) {

        Assert.notNull(dto.getUserId(), "用户ID不能为空");
        Assert.hasText(dto.getPlateNo(), "车牌号不能为空");
        Assert.notNull(dto.getSpaceId(), "请选择关联车位");

        // 1. 保存车辆基本信息（如果不存在）
        Vehicle exist = this.lambdaQuery()
                .eq(Vehicle::getPlateNo, dto.getPlateNo())
                .one();

        if (exist != null) {
            if (!exist.getUserId().equals(dto.getUserId())) {
                throw new RuntimeException("该车牌已被其他用户绑定");
            }
            // 已存在且是自己的，更新信息
            if (dto.getColor() != null) exist.setColor(dto.getColor());
            if (dto.getBrand() != null) exist.setBrand(dto.getBrand());
            this.updateById(exist);
        } else {
            // 新增车辆
            Vehicle vehicle = new Vehicle();
            vehicle.setUserId(dto.getUserId());
            vehicle.setPlateNo(dto.getPlateNo());
            vehicle.setBrand(dto.getBrand());
            vehicle.setColor(dto.getColor());
            vehicle.setStatus("ACTIVE");
            vehicle.setCreateTime(LocalDateTime.now());
            vehicle.setUpdateTime(LocalDateTime.now());
            this.save(vehicle);
        }

        // 2. 绑定到指定车位 (biz_parking_space_plate)
        // 2.1 校验车位是否存在且可用
        ParkingSpace space = spaceMapper.selectById(dto.getSpaceId());
        if (space == null) {
            throw new RuntimeException("车位不存在");
        }
        if (!isBindableSpaceStatus(space.getStatus())) {
            throw new RuntimeException("该车位已被预订或占用，不可绑定");
        }

        // 2.2 检查该车位下是否已绑定该车牌
        Long count = plateMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ParkingSpacePlate>()
                .eq(ParkingSpacePlate::getSpaceId, dto.getSpaceId())
                .eq(ParkingSpacePlate::getPlateNo, dto.getPlateNo())
                .ne(ParkingSpacePlate::getStatus, "REJECTED")); // 拒绝的可以重提

        if (count > 0) {
            throw new RuntimeException("该车位已绑定或正在审核此车牌");
        }

        // 2.3 更新车位状态为 RESERVED (预订中)
        space.setStatus("RESERVED");
        space.setUpdateTime(LocalDateTime.now());
        spaceMapper.updateById(space);

        // 2.4 插入车位-车牌关联记录，状态设为 PENDING
        ParkingSpacePlate plateBind = new ParkingSpacePlate();
        plateBind.setSpaceId(dto.getSpaceId());
        plateBind.setUserId(dto.getUserId());
        plateBind.setPlateNo(dto.getPlateNo());
        plateBind.setStatus("PENDING"); 
        plateBind.setCreateTime(LocalDateTime.now());
        plateBind.setUpdateTime(LocalDateTime.now());
        
        plateMapper.insert(plateBind);
    }

    @Override
    public IPage<ParkingCarAuditVO> listAudit(String status, Integer pageNum, Integer pageSize) {
        Page<ParkingSpacePlate> page = new Page<>(pageNum, pageSize);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ParkingSpacePlate> query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            query.eq(ParkingSpacePlate::getStatus, status);
        }
        query.orderByDesc(ParkingSpacePlate::getCreateTime);
        
        IPage<ParkingSpacePlate> platePage = plateMapper.selectPage(page, query);
        
        return platePage.convert(item -> {
            ParkingCarAuditVO vo = new ParkingCarAuditVO();
            vo.setId(item.getId());
            vo.setPlateNo(item.getPlateNo());
            vo.setStatus(item.getStatus());
            vo.setRejectReason(item.getRejectReason());
            vo.setCreateTime(item.getCreateTime());
            
            try {
                // 填充申请人
                if (item.getUserId() != null) {
                    UserDTO user = userServiceClient.getUserById(item.getUserId());
                    if (user != null) {
                        vo.setUserName(user.getRealName());
                    }
                }
                
                // 填充车位号
                if (item.getSpaceId() != null) {
                    ParkingSpace space = spaceMapper.selectById(item.getSpaceId());
                    if (space != null) {
                        vo.setSpaceNo(space.getSpaceNo());
                    }
                }
            } catch (Exception e) {
                // 即使关联查询失败，也不影响主数据显示
                e.printStackTrace();
            }
            
            return vo;
        });
    }

    @Override
    @Transactional
    public void auditCar(ParkingCarAuditDTO dto) {
        ParkingSpacePlate plate = plateMapper.selectById(dto.getId());
        if (plate == null) {
            throw new RuntimeException("申请记录不存在");
        }
        if (!"PENDING".equals(plate.getStatus())) {
            throw new RuntimeException("该记录已审核");
        }

        String auditStatus = dto.getStatus();
        LocalDateTime now = LocalDateTime.now();
        if ("APPROVED".equals(auditStatus)) {
            plate.setStatus("AWAITING_PAYMENT");
            plate.setRejectReason(null);

            ParkingSpace space = spaceMapper.selectById(plate.getSpaceId());
            if (space != null && isBindableSpaceStatus(space.getStatus())) {
                space.setStatus("RESERVED");
                space.setUpdateTime(now);
                spaceMapper.updateById(space);
            }
        } else if ("REJECTED".equals(auditStatus)) {
            plate.setStatus("REJECTED");
            plate.setRejectReason(dto.getRejectReason());

            ParkingSpace space = spaceMapper.selectById(plate.getSpaceId());
            if (space != null && "RESERVED".equals(space.getStatus())) {
                Long otherCount = plateMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ParkingSpacePlate>()
                                .eq(ParkingSpacePlate::getSpaceId, plate.getSpaceId())
                                .ne(ParkingSpacePlate::getId, plate.getId())
                                .in(ParkingSpacePlate::getStatus, "PENDING", "AWAITING_PAYMENT", "ACTIVE")
                );
                if (otherCount == null || otherCount == 0) {
                    space.setStatus("AVAILABLE");
                    space.setUpdateTime(now);
                    spaceMapper.updateById(space);
                }
            }
        } else {
            throw new RuntimeException("非法审核状态：" + auditStatus);
        }

        plate.setUpdateTime(now);
        plateMapper.updateById(plate);
    }

    private boolean isBindableSpaceStatus(String status) {
        return status == null || status.trim().isEmpty() || BINDABLE_SPACE_STATUS.contains(status);
    }
}
