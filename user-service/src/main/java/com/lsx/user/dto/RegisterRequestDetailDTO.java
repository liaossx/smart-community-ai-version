package com.lsx.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterRequestDetailDTO {
    private Long id;
    private String username;
    private String phone;
    private String realName;
    private String role;
    private String status;
    private Long communityId;
    private LocalDateTime applyTime;
    private LocalDateTime approveTime;
    private Long approveBy;
    private String rejectReason;
}
