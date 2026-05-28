package com.lsx.property.notice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeVO {
    private Long id;
    private String title;
    private String content;
    private String targetType;
    private String communityName;
    private String buildingNo;
    private Boolean topFlag;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Boolean read;
}


