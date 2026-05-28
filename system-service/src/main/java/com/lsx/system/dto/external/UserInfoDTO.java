package com.lsx.system.dto.external;

import lombok.Data;

@Data
public class UserInfoDTO {
    private Long userId;
    private String username;
    private String name;
    private String role;
}
