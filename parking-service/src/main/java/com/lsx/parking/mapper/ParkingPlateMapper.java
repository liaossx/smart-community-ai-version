package com.lsx.parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.parking.entity.ParkingSpacePlate;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface ParkingPlateMapper extends BaseMapper<ParkingSpacePlate> {
    // 查询鏌愯溅浣嶅拰车牌鐨勮褰?    @Select("SELECT * FROM biz_parking_space_plate WHERE space_id = #{spaceId} AND plate_no = #{plateNo} LIMIT 1")
    ParkingSpacePlate selectBySpaceAndPlate(@Param("spaceId") Long spaceId, @Param("plateNo") String plateNo);

    // 查询鏌愯溅浣嶄笅用户绑定鐨勬墍鏈?ACTIVE 车牌
    @Select("SELECT * FROM biz_parking_space_plate WHERE space_id = #{spaceId} AND user_id = #{userId} AND status = 'ACTIVE'")
    List<ParkingSpacePlate> selectActivePlatesBySpace(@Param("spaceId") Long spaceId, @Param("userId") Long userId);

    // 鎻掑叆
    @Insert("INSERT INTO biz_parking_space_plate(space_id, user_id, plate_no, status, create_time, update_time) " +
            "VALUES(#{spaceId}, #{userId}, #{plateNo}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ParkingSpacePlate plate);

    // 更新
    @Update("UPDATE biz_parking_space_plate SET status = #{status}, reject_reason = #{rejectReason}, update_time = #{updateTime} WHERE id = #{id}")
    int updateById(ParkingSpacePlate plate);
}

