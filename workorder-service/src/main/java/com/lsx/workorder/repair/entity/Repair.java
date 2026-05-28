package com.lsx.workorder.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_repair")
public class Repair {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long communityId;
    private Long houseId;
    private String faultType;
    private String faultDesc;
    private String faultImgs;
    private String status;
    private String handleRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
