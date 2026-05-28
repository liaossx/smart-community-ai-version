package com.lsx.user.dto;

import lombok.Data;

@Data
public class AdminUpdateUserDTO {
    private Long userId;
    private String realName;
    private String phone;
    private String role;
    private Long communityId;
}

