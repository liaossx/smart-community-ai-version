package com.lsx.property.visitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.visitor.entity.SysVisitor;
import com.lsx.property.visitor.mapper.SysVisitorMapper;
import com.lsx.property.visitor.service.VisitorService;
import com.lsx.property.client.UserServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.lsx.property.visitor.dto.VisitorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class VisitorServiceImpl extends ServiceImpl<SysVisitorMapper, SysVisitor> implements VisitorService {
    
    @Autowired
    private UserServiceClient userServiceClient;

    private static final Logger log = LoggerFactory.getLogger(VisitorServiceImpl.class);

    @Override
    public Long apply(SysVisitor entity) {
        Long cid = UserContext.getCommunityId();
        entity.setCommunityId(cid);
        entity.setStatus("PENDING");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        this.save(entity);
        return entity.getId();
    }

    @Override
    public IPage<SysVisitor> myList(Long userId, Integer pageNum, Integer pageSize) {
        Page<SysVisitor> page = new Page<>(pageNum, pageSize);
        return this.page(page, new QueryWrapper<SysVisitor>().eq("user_id", userId).orderByDesc("create_time"));
    }

    @Override
    public IPage<VisitorDTO> adminList(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<VisitorDTO> page = new Page<>(pageNum, pageSize);
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        
        log.info("查询访客列表: communityId={}, role={}, keyword={}, status={}", cid, role, keyword, status);
        
        Long filterCid = null;
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null) filterCid = cid;
            else filterCid = -1L;
        }
        
        try {
            IPage<VisitorDTO> result = baseMapper.selectAdminList(page, status, keyword, filterCid);
            // 填充 ownerName (业主姓名)
            result.getRecords().forEach(dto -> {
                if (dto.getUserId() != null) {
                    try {
                        String realName = userServiceClient.getRealNameById(dto.getUserId());
                        dto.setOwnerName(realName);
                    } catch (Exception e) {
                        log.warn("获取业主姓名失败: userId={}", dto.getUserId());
                        dto.setOwnerName("未知业主");
                    }
                }
            });
            log.info("查询结果数量: size={}", result.getRecords().size());
            return result;
        } catch (Exception e) {
            log.error("查询失败", e);
            throw e;
        }
    }

    @Override
    public boolean audit(Long id, String status, String remark) {
        SysVisitor v = this.getById(id);
        if (v == null) return false;
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid == null || v.getCommunityId() == null || !cid.equals(v.getCommunityId())) {
                throw new RuntimeException("无权审核其他社区的访客");
            }
        }
        v.setStatus(status);
        v.setAuditRemark(remark);
        v.setUpdateTime(LocalDateTime.now());
        return this.updateById(v);
    }
}
