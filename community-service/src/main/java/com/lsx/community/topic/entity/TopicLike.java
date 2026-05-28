package com.lsx.community.topic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_topic_like")
public class TopicLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private Long userId;
    private LocalDateTime createTime;
}
