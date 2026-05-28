package com.lsx.community.topic.dto;

import lombok.Data;

@Data
public class TopicCommentDTO {
    private Long topicId;
    private Long parentId;
    private String content;
}
