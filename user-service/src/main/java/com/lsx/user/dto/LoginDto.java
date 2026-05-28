package com.lsx.user.dto;

import lombok.Data;

@Data
public class LoginDto {
    private String username;
    private String password;
    private String role;
}

