package com.lsx.user.client;

import com.lsx.user.dto.external.HouseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "house-service", path = "/api/house")
public interface HouseServiceClient {

    @GetMapping("/user/{userId}")
    List<HouseDTO> getHousesByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/{id}")
    HouseDTO getHouseById(@PathVariable("id") Long id);
    
    @PostMapping("/bind")
    Boolean bindUserToHouse(@RequestParam("userId") Long userId, @RequestParam("houseId") Long houseId);
}
