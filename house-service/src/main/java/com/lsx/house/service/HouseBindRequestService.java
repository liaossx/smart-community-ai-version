package com.lsx.house.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.house.entity.HouseBindRequest;

public interface HouseBindRequestService extends IService<HouseBindRequest> {
    Page<HouseBindRequest> pageRequests(Integer pageNum, Integer pageSize, String keyword, String status, Long communityId);

    boolean approve(Long id, String identityType, Long adminId);

    boolean reject(Long id, String reason, Long adminId);
}
