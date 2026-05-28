package com.lsx.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_register_request")
public class UserRegisterRequest {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String realName;
    private String phone;
    private String role;
    private String status; // PENDING / ACTIVE / REJECTED
    private Long communityId;

    private LocalDateTime applyTime;
    private LocalDateTime approveTime;
    private Long approveBy;
    private String rejectReason;
}
