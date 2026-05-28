package com.lsx.community.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.community.activity.entity.SysActivity;
import com.lsx.community.activity.dto.SignupRecordDTO;

public interface ActivityService extends IService<SysActivity> {
    IPage<SysActivity> list(String status, Integer pageNum, Integer pageSize);
    SysActivity detail(Long id);
    Long publish(SysActivity a);
    boolean deleteByIdWithCheck(Long id);
    boolean join(Long activityId, Long userId);
    IPage<SignupRecordDTO> getSignupList(Long activityId, Integer pageNum, Integer pageSize);
}
