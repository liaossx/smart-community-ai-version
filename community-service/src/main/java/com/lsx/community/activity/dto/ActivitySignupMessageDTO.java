package com.lsx.community.activity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ActivitySignupMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private Long userId;
}