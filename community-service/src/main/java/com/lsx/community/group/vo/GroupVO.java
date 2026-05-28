package com.lsx.community.group.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupVO {
    private Long id;
    private String title;
    private String content;
    private List<String> images;
    private BigDecimal price;
    private Integer targetCount;
    private Integer currentCount;
    private LocalDateTime endTime;
    private String status;
    private String creatorName;
    private LocalDateTime createTime;
}
