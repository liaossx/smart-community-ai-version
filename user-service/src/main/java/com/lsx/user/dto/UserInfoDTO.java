package com.lsx.user.dto;


import lombok.Data;

@Data
public class UserInfoDTO {
    private Long id;              // 为了兼容前端，统一返回 id
    private Long userId;          // 用户ID（对应sys_user.id，保留兼容老接口）
    private String username;      // 用户名
    private String name;          // 真实姓名（对应realName）
    private String community;     // 社区名（来自sys_house）
    private String room;          // 房屋号（如"1栋101"，来自sys_house）
    private String role;          // 用户角色
    private String phone;
    private Long communityId;
}
