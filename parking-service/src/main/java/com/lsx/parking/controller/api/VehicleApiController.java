package com.lsx.parking.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.parking.entity.Vehicle;
import com.lsx.parking.mapper.VehicleMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/parking/vehicle")
public class VehicleApiController {

    @Resource
    private VehicleMapper vehicleMapper;

    @GetMapping("/user/{userId}")
    public List<Vehicle> getVehiclesByUserId(@PathVariable("userId") Long userId) {
        return vehicleMapper.selectList(
                new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getUserId, userId)
        );
    }
}

