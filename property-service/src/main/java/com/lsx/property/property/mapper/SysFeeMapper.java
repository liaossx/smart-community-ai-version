package com.lsx.property.property.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.property.property.dto.FeeDTO;
import com.lsx.property.property.entity.SysFee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysFeeMapper extends BaseMapper<SysFee> {
    
    @Select("<script>" +
            "SELECT f.*, " +
            "ANY_VALUE(u.real_name) AS ownerName, " +
            "ANY_VALUE(h.building_no) AS buildingNo, " +
            "ANY_VALUE(h.house_no) AS houseNo " +
            "FROM sys_fee f " +
            "LEFT JOIN sys_house h ON f.house_id = h.id " +
            "LEFT JOIN user_house uh ON h.id = uh.house_id AND uh.status = 'approved' " +
            "LEFT JOIN sys_user u ON uh.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='status != null and status.length > 0'> AND f.status = #{status} </if>" +
            "<if test='communityId != null'> AND f.community_id = #{communityId} </if>" +
            "<if test='ownerName != null and ownerName.length > 0'> AND u.real_name LIKE concat('%', #{ownerName}, '%') </if>" +
            "GROUP BY f.id " + 
            "ORDER BY f.create_time DESC" +
            "</script>")
    IPage<FeeDTO> selectAdminList(Page<FeeDTO> page, 
                                  @Param("status") String status, 
                                  @Param("ownerName") String ownerName,
                                  @Param("communityId") Long communityId);
}
