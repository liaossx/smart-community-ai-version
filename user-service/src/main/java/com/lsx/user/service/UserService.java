package com.lsx.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.user.dto.*;
import com.lsx.user.entity.User;
import com.lsx.user.vo.LoginResult;
import com.lsx.user.vo.RegisterResult;

public interface UserService extends IService<User> {
    // 登录方法：根据用户名和密码查询用户（带角色校验）
    LoginResult login(LoginDto loginDto);

    // 注册方法：添加用户 用户注册时提交 用户名、密码、手机号、绑定的房屋ID
    RegisterResult register(RegisterDto registerDto);

    //绑定房屋方法 ：根据用户ID和房屋ID绑定用户和房屋
    void bindUserToHouse(Long userId, Long houseId);

    /**
     * 分页获取用户列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param keyword 搜索关键词（姓名/手机号）
     * @param role 角色筛选
     * @return 分页结果
     */
    Page<UserDTO> getUserList(Integer pageNum, Integer pageSize, String keyword, String role);

    /**
     * 获取用户详情
     * @param userId 用户ID
     * @return 用户详情（包含房屋、车辆信息）
     */
    UserDetailDTO getUserDetail(Long userId);

    /**
     * 管理员修改用户信息
     * @param dto 修改信息DTO
     */
    void adminUpdateUser(AdminUpdateUserDTO dto);

    /**
     * 修改用户状态
     * @param userId 用户ID
     * @param status 状态 0:禁用 1:正常
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 重置密码
     * @param userId 用户ID
     */
    void resetPassword(Long userId, String newPassword);

    boolean updateCommunityIdIfEmpty(Long userId, Long communityId);
}

