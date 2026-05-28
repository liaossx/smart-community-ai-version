package com.lsx.property.notice.dto;

import lombok.Data;

@Data
public class AdminNoticeListItemDTO {
    private Long id;
    private String title;
    private String publishStatus;
    private Boolean topFlag;
    private String targetType;
    private String communityName;
    private String buildingNo;
    private String publishTime;
    private String expireTime;
    private String creatorName;
    private Integer readCount;
    private Integer totalCount;
}


