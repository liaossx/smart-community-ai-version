package com.lsx.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.house.entity.House;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HouseMapper extends BaseMapper<House> {
}
