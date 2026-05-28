package com.lsx.community.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.community.activity.dto.SignupRecordDTO;
import com.lsx.community.activity.entity.SysActivitySignup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysActivitySignupMapper extends BaseMapper<SysActivitySignup> {
    
    @Select("SELECT s.id, u.real_name AS userName, u.phone AS userPhone, s.signup_time AS signupTime, 'SIGNED' AS status " +
            "FROM sys_activity_signup s " +
            "LEFT JOIN sys_user u ON s.user_id = u.id " +
            "WHERE s.activity_id = #{activityId} " +
            "ORDER BY s.signup_time DESC")
    IPage<SignupRecordDTO> selectSignupList(Page<SignupRecordDTO> page, @Param("activityId") Long activityId);
}
