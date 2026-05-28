package com.lsx.house.dto.external;

import lombok.Data;

@Data
public class UserInfoDTO {
    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String phone;
    private String role;
}
