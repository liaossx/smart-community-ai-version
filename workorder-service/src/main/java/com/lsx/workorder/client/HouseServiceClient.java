package com.lsx.workorder.client;

import com.lsx.workorder.dto.external.HouseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "house-service", path = "/api/house")
public interface HouseServiceClient {

    @GetMapping("/{id}")
    HouseDTO getHouseById(@PathVariable("id") Long id);

    @GetMapping("/user/{userId}")
    List<HouseDTO> getHousesByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/list/ids")
    List<HouseDTO> getHouseListByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/search")
    List<Long> searchHouseIds(@RequestParam("keyword") String keyword);

    @GetMapping("/community/count")
    Long countCommunities();

    @GetMapping("/owner/count/community")
    Long countOwnersByCommunityId(@RequestParam("communityId") Long communityId);

    @GetMapping("/community/{id}/name")
    String getCommunityNameById(@PathVariable("id") Long id);

    @GetMapping("/user/count/condition")
    Integer countUsersByCondition(
            @RequestParam(value = "communityName", required = false) String communityName,
            @RequestParam(value = "buildingNo", required = false) String buildingNo);

    @GetMapping("/info")
    HouseDTO getHouseByInfo(
            @RequestParam(value = "buildingNo", required = false) String buildingNo,
            @RequestParam(value = "houseNo", required = false) String houseNo);

    @GetMapping("/bound-users/{houseId}")
    List<Long> getBoundUsersByHouseId(@PathVariable("houseId") Long houseId);

    @GetMapping("/bound-ids")
    List<Long> getBoundHouseIds(
            @RequestParam(value = "communityName", required = false) String communityName,
            @RequestParam(value = "buildingNo", required = false) String buildingNo);
}
