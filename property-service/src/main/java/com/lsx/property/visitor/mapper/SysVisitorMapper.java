package com.lsx.property.visitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.property.visitor.dto.VisitorDTO;
import com.lsx.property.visitor.entity.SysVisitor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysVisitorMapper extends BaseMapper<SysVisitor> {

    @Select("<script>" +
            "SELECT v.*, " +
            "MAX(h.building_no) AS buildingNo, MAX(h.house_no) AS houseNo " +
            "FROM sys_visitor v " +
            "LEFT JOIN user_house uh ON v.user_id = uh.user_id AND uh.status = 'approved' " +
            "LEFT JOIN sys_house h ON uh.house_id = h.id " +
            "WHERE 1=1 " +
            "<if test='status != null and status.length > 0'> AND v.status = #{status} </if>" +
            "<if test='keyword != null and keyword.length > 0'> AND (v.visitor_name LIKE concat('%', #{keyword}, '%') OR v.visitor_phone LIKE concat('%', #{keyword}, '%')) </if>" +
            "<if test='communityId != null'> AND v.community_id = #{communityId} </if>" +
            "GROUP BY v.id " + 
            "ORDER BY v.create_time DESC" +
            "</script>")
    IPage<VisitorDTO> selectAdminList(Page<VisitorDTO> page, 
                                      @Param("status") String status, 
                                      @Param("keyword") String keyword,
                                      @Param("communityId") Long communityId);
}

