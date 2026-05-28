package com.lsx.user.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String role;
    private Long communityId;
    private Integer status;
    private LocalDateTime createTime;
}

