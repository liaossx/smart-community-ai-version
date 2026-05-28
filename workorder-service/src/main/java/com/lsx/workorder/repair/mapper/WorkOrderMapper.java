package com.lsx.workorder.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.workorder.repair.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单表 Mapper 接口
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {
}
