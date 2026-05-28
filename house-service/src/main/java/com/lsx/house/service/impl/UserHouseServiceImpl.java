package com.lsx.house.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.house.entity.House;
import com.lsx.house.entity.UserHouse;
import com.lsx.house.mapper.HouseMapper;
import com.lsx.house.mapper.UserHouseMapper;
import com.lsx.house.service.UserHouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserHouseServiceImpl extends ServiceImpl<UserHouseMapper, UserHouse> implements UserHouseService {
    @Autowired
    private UserHouseMapper userHouseMapper;
    @Autowired
    private HouseMapper houseMapper;

    @Override
    public List<House> getHouseByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<UserHouse> wrapper = new LambdaQueryWrapper<UserHouse>()
                .select(UserHouse::getHouseId)
                .eq(UserHouse::getUserId, userId);
        
        List<UserHouse> userHouses = userHouseMapper.selectList(wrapper);
        if (userHouses.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> houseIds = userHouses.stream()
                .map(UserHouse::getHouseId)
                .distinct()
                .collect(Collectors.toList());
                
        if (houseIds.isEmpty()) {
            return Collections.emptyList();
        }

        return houseMapper.selectBatchIds(houseIds);
    }
}
