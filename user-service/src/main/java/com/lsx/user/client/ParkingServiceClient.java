package com.lsx.user.client;

import com.lsx.user.dto.external.VehicleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "parking-service", path = "/api/parking")
public interface ParkingServiceClient {

    @GetMapping("/vehicle/user/{userId}")
    List<VehicleDTO> getVehiclesByUserId(@PathVariable("userId") Long userId);
}
