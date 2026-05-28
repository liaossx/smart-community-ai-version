package com.lsx.community.group.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_group_activity")
public class GroupActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long communityId;
    private String title;
    private String content;
    private String images;
    private BigDecimal price;
    private Integer targetCount;
    private Integer currentCount;
    private LocalDateTime endTime;
    private String status;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
