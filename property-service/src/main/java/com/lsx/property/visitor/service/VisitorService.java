package com.lsx.property.visitor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.property.visitor.entity.SysVisitor;

import com.lsx.property.visitor.dto.VisitorDTO;

public interface VisitorService extends IService<SysVisitor> {
    Long apply(SysVisitor entity);
    IPage<SysVisitor> myList(Long userId, Integer pageNum, Integer pageSize);
    IPage<VisitorDTO> adminList(Integer pageNum, Integer pageSize, String status, String keyword);
    boolean audit(Long id, String status, String remark);
}

