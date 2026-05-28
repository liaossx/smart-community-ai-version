package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.entity.ParkingSpaceLease;
import com.lsx.parking.entity.ParkingSpacePlate;
import com.lsx.parking.mapper.ParkingPlateMapper;
import com.lsx.parking.mapper.ParkingSpaceLeaseMapper;
import com.lsx.parking.service.ParkingPlateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingPlateServiceImpl extends ServiceImpl<ParkingPlateMapper, ParkingSpacePlate> implements ParkingPlateService {

    private final ParkingPlateMapper plateMapper;
    private final ParkingSpaceLeaseMapper leaseMapper;

    public void bindPlate(Long userId, Long spaceId, String plateNo) {
        // 校验用户是否拥有该车位
        ParkingSpaceLease lease = leaseMapper.getActiveLease(userId, spaceId);
        if (lease == null) {
            throw new RuntimeException("你没有权限绑定该车位");
        }

        // 已存在则启用
        ParkingSpacePlate plate = plateMapper.selectBySpaceAndPlate(spaceId, plateNo);
        if (plate != null) {
            plate.setStatus("ACTIVE");
            plate.setUpdateTime(LocalDateTime.now());
            plateMapper.updateById(plate);
        } else {
            plate = new ParkingSpacePlate();
            plate.setSpaceId(spaceId);
            plate.setPlateNo(plateNo);
            plate.setUserId(userId);
            plate.setStatus("ACTIVE");
            plate.setCreateTime(LocalDateTime.now());
            plate.setUpdateTime(LocalDateTime.now());
            plateMapper.insert(plate);
        }
    }

    public void unbindPlate(Long userId, Long spaceId, String plateNo) {
        ParkingSpacePlate plate = plateMapper.selectBySpaceAndPlate(spaceId, plateNo);
        if (plate == null || !plate.getUserId().equals(userId)) {
            throw new RuntimeException("你没有权限解绑该车牌");
        }
        plate.setStatus("DISABLED");
        plate.setUpdateTime(LocalDateTime.now());
        plateMapper.updateById(plate);
    }

    public List<String> getPlatesBySpace(Long userId, Long spaceId) {
        List<ParkingSpacePlate> plates = plateMapper.selectActivePlatesBySpace(spaceId, userId);
        return plates.stream().map(ParkingSpacePlate::getPlateNo).collect(Collectors.toList());
    }
}
