package com.lsx.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.house.entity.UserHouse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserHouseMapper extends BaseMapper<UserHouse> {

    @Select("SELECT COUNT(DISTINCT user_id) FROM user_house uh " +
            "LEFT JOIN sys_house h ON uh.house_id = h.id " +
            "WHERE h.community_id = #{communityId} AND uh.status IN ('approved','APPROVED','1','审核通过')")
    Long countDistinctOwnerByCommunityId(@Param("communityId") Long communityId);

    @Insert("INSERT INTO user_house (user_id, house_id, status, create_time) VALUES (#{userId}, #{houseId}, 'pending', NOW())")
    int insertUserHouseBind(@Param("userId") Long userId, @Param("houseId") Long houseId);

    @Insert("INSERT INTO user_house (user_id, house_id, status, create_time) VALUES (#{userId}, #{houseId}, 'approved', NOW())")
    int insertUserHouseApproved(@Param("userId") Long userId, @Param("houseId") Long houseId);
}
