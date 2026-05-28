package com.lsx.property.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_visitor")
public class SysVisitor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long communityId;
    private String visitorName;
    private String visitorPhone;
    private String reason;
    private LocalDateTime visitTime;
    private String carNo;
    private String status;
    private String auditRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

