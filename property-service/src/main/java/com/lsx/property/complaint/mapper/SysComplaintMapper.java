package com.lsx.property.complaint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.property.complaint.dto.ComplaintDTO;
import com.lsx.property.complaint.entity.SysComplaint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysComplaintMapper extends BaseMapper<SysComplaint> {
    // 移除联表查询，因为微服务拆分后不能跨库联表
}

