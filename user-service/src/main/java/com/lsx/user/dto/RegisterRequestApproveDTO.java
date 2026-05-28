package com.lsx.user.dto;

import lombok.Data;

@Data
public class RegisterRequestApproveDTO {
    private String role;
    private Long communityId;
}
