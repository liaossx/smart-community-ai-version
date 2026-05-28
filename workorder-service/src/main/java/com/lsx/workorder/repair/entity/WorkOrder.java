package com.lsx.workorder.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lsx.workorder.repair.dto.RepairInfoDTO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单表实体类
 */
@Data
@TableName("biz_work_order")
public class WorkOrder {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 关联报修单ID */
    private Long repairId;
    
    /** 工单编号 */
    private String orderNo;
    
    /** 所属社区ID */
    private Long communityId;
    
    /** 维修员ID */
    private Long workerId;
    
    /** 维修员姓名 */
    private String workerName;
    
    /** 维修员电话 */
    private String workerPhone;
    
    /** 状态: PENDING-待指派, ASSIGNED-已指派, PROCESSING-处理中, COMPLETED-已完成, CANCELLED-已取消 */
    private String status;
    
    /** 优先级: 1-普通, 2-紧急, 3-特急 */
    private Integer priority;
    
    /** 计划开始时间 */
    private LocalDateTime planStartTime;
    
    /** 实际开始时间 */
    private LocalDateTime actualStartTime;
    
    /** 实际结束时间 */
    private LocalDateTime actualEndTime;
    
    /** 处理结果 */
    private String processResult;
    
    /** 处理后图片 */
    private String processImgs;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private RepairInfoDTO repairInfo;
}
