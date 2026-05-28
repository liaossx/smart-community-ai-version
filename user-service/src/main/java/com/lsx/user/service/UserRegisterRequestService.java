package com.lsx.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.user.entity.UserRegisterRequest;

public interface UserRegisterRequestService extends IService<UserRegisterRequest> {
    Page<UserRegisterRequest> pageRequests(Integer pageNum, Integer pageSize, String keyword, String status, String role);

    boolean approve(Long id, String role, Long communityId, Long adminId);

    boolean reject(Long id, String reason, Long adminId);
}
