package com.lsx.community.dto.external;

import lombok.Data;

@Data
public class UserInfoDTO {
    private Long userId;
    private String username;
    private String name;
    private String community;
    private String room;
    private String role;
    private String phone;
}
