package com.lsx.parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.parking.entity.ParkingSpaceLease;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ParkingSpaceLeaseMapper extends BaseMapper<ParkingSpaceLease> {

    default ParkingSpaceLease selectActiveLeaseByUser(Long userId) {
        return this.selectOne(
                Wrappers.<ParkingSpaceLease>lambdaQuery()
                        .eq(ParkingSpaceLease::getUserId, userId)
                        .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                        .last("LIMIT 1")
        );
    }
    @Select("SELECT * FROM biz_parking_space_lease WHERE user_id = #{userId} AND space_id = #{spaceId} AND status = 'ACTIVE' LIMIT 1")
    ParkingSpaceLease getActiveLease(@Param("userId") Long userId, @Param("spaceId") Long spaceId);
}

