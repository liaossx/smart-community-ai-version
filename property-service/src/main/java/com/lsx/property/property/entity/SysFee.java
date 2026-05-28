package com.lsx.property.property.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_fee")
public class SysFee {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 主键ID
    private Long houseId;            // 房屋ID
    private Long communityId;        // 社区ID
    private String buildingNo;       // 楼栋号
    private String feeCycle;         // 费用周期(如2025-01)
    private BigDecimal feeAmount;    // 费用金额
    private String feeType = "物业费"; // 费用类型
    private String status = "UNPAID";// 状态 UNPAID/PAID/OVERDUE
    
    private Integer remindCount = 0; // 提醒次数

    private LocalDateTime dueDate;  // 截止日期

    private LocalDateTime createTime;// 创建时间
    private LocalDateTime updateTime;// 更新时间
    private String remark;           // 备注
}
