package com.lsx.community.topic.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TopicCommentVO {
    private Long id;
    private String content;
    private Long userId;
    private String userName;
    private String userAvatar;
    private LocalDateTime createTime;
}
