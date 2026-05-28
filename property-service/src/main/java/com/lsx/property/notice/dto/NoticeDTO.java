package com.lsx.property.notice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoticeDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime publishTime;
    private String communityName;
    private Boolean topFlag;
    private Boolean read; // 是否已读
}
