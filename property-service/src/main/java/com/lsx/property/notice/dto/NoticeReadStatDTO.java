package com.lsx.property.notice.dto;

import lombok.Data;

@Data
public class NoticeReadStatDTO {
    private Long noticeId;
    private String title;
    private Integer totalUsers;
    private Integer readCount;
    private Integer unreadCount;
    private Double readRate;
}


