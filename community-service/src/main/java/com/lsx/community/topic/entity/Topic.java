package com.lsx.community.topic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_topic")
public class Topic {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long communityId;
    private Long userId;
    private String title;
    private String content;
    private String images;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
