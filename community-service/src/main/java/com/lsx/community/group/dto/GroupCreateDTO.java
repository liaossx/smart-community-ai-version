package com.lsx.community.group.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GroupCreateDTO {
    private String title;
    private String content;
    private String images;
    private BigDecimal price;
    private Integer targetCount;
    private LocalDateTime endTime;
}
