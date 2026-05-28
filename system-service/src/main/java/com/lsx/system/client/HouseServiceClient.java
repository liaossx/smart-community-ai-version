package com.lsx.system.client;

import com.lsx.system.dto.external.HouseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "house-service", path = "/api/house", fallbackFactory = com.lsx.system.client.fallback.HouseServiceClientFallbackFactory.class)
public interface HouseServiceClient {

    @GetMapping("/{id}")
    HouseDTO getHouseById(@PathVariable("id") Long id);

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
}
