package com.lsx.property.complaint.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.property.complaint.entity.SysComplaint;

import com.lsx.property.complaint.dto.ComplaintDTO;

public interface ComplaintService extends IService<SysComplaint> {
    Long submit(SysComplaint c);
    IPage<SysComplaint> my(Long userId, Integer pageNum, Integer pageSize);
    IPage<ComplaintDTO> adminList(Integer pageNum, Integer pageSize, String status);
    boolean handle(Long id, String result);
}

