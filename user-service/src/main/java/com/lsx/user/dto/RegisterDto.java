package com.lsx.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class RegisterDto {
    @NotBlank(message = "用户名不能为空")
    private String username;  // 登录用户名
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6到20位之间")
    private String password;  // 登录密码（MVP阶段暂存明文）
    private String realName;  // 真实姓名
    private String phone;     // 手机号
    @NotBlank(message = "角色不能为空")
    private String role;      // 角色：owner（业主）、admin（管理员）
    private Long communityId; // 归属社区ID
}

