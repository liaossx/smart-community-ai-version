package com.lsx.community.topic.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TopicVO {
    private Long id;
    private String title;
    private String content;
    private List<String> images;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isLiked;
    private LocalDateTime createTime;
}
