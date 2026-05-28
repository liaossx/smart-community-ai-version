package com.lsx.house.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.house.entity.House;
import com.lsx.house.vo.HouseResult;

import java.util.List;

public interface HouseService extends IService<House> {
    HouseResult getHouseInfoById(Long houseId);
    List<HouseResult> getAllHouseInfo();
    void updateHouseStatus(Long id, String status);
}
