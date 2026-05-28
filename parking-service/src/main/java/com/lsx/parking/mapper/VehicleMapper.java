package com.lsx.parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.parking.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {

    default Vehicle selectByPlateNo(String plateNo) {
        return this.selectOne(
                Wrappers.<Vehicle>lambdaQuery()
                        .eq(Vehicle::getPlateNo, plateNo)
                        .eq(Vehicle::getStatus, "ACTIVE")
        );
    }
}

