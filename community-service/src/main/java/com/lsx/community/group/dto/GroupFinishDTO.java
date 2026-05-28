package com.lsx.community.group.dto;

import lombok.Data;

@Data
public class GroupFinishDTO {
    private Long activityId;
    private String status; // FINISHED, CANCELLED
}
