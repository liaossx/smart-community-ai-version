package com.lsx.property.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SysNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long communityId;
    private String communityName;
    private String title;
    private String content;
    private String publishStatus; // DRAFT/PUBLISHED
    private String targetType; // ALL, COMMUNITY, BUILDING, USER
    private Long targetUserId;
    private String targetBuilding; // 楼栋号
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Boolean topFlag;
    private Long creatorId;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
