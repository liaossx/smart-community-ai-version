package com.lsx.house.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.house.entity.House;
import com.lsx.house.entity.UserHouse;

import java.util.List;

public interface UserHouseService extends IService<UserHouse> {
    List<House> getHouseByUserId(Long userId);
}
