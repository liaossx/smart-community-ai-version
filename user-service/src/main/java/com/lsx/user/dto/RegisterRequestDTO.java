package com.lsx.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterRequestDTO {
    private Long id;
    private String username;
    private String phone;
    private String realName;
    private String role;
    private String status;
    private LocalDateTime applyTime;
}
