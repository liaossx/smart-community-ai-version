package com.lsx.house.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.house.entity.House;
import com.lsx.house.entity.UserHouse;
import com.lsx.house.mapper.HouseMapper;
import com.lsx.house.mapper.UserHouseMapper;
import com.lsx.house.service.HouseService;
import com.lsx.house.vo.HouseResult;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HouseServiceImpl extends ServiceImpl<HouseMapper, House> implements HouseService {

    @Resource
    private UserHouseMapper userHouseMapper;

    @Override
    @Cacheable(cacheNames = "houseInfo", key = "#houseId")
    public HouseResult getHouseInfoById(Long houseId) {
        House house = baseMapper.selectById(houseId);
        if (house == null) return null;
        HouseResult result = new HouseResult();
        BeanUtils.copyProperties(house, result);
        return result;
    }

    @Override
    @Cacheable(cacheNames = "houseInfo", key = "'all'")
    public List<HouseResult> getAllHouseInfo() {
        return baseMapper.selectList(null).stream().map(house -> {
            HouseResult result = new HouseResult();
            BeanUtils.copyProperties(house, result);
            return result;
        }).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = "houseInfo", allEntries = true)
    public void updateHouseStatus(Long id, String status) {
        // id 是 UserHouse 的 id
        UserHouse userHouse = userHouseMapper.selectById(id);
        if (userHouse == null) {
            throw new RuntimeException("申请记录不存在");
        }
        
        userHouse.setStatus(status);
        userHouseMapper.updateById(userHouse);
        
        // 如果审核通过，更新 House 的绑定状态
        if ("1".equals(status) || "approved".equalsIgnoreCase(status) || "审核通过".equals(status)) {
             House house = baseMapper.selectById(userHouse.getHouseId());
             if (house != null) {
                 house.setBindStatus(1);
                 baseMapper.updateById(house);
             }
        }
    }
}
