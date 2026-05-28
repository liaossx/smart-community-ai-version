package com.lsx.property.complaint.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.complaint.entity.SysComplaint;
import com.lsx.property.complaint.mapper.SysComplaintMapper;
import com.lsx.property.complaint.service.ComplaintService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.lsx.property.complaint.dto.ComplaintDTO;
import com.lsx.property.client.UserServiceClient;
import com.lsx.property.dto.external.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Collectors;

@Service
public class ComplaintServiceImpl extends ServiceImpl<SysComplaintMapper, SysComplaint> implements ComplaintService {

    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    public Long submit(SysComplaint c) {
        Long cid = UserContext.getCommunityId();
        c.setCommunityId(cid);
        c.setStatus("PENDING");
        c.setCreateTime(LocalDateTime.now());
        this.save(c);
        return c.getId();
    }

    @Override
    public IPage<SysComplaint> my(Long userId, Integer pageNum, Integer pageSize) {
        Page<SysComplaint> page = new Page<>(pageNum, pageSize);
        return this.page(page, new QueryWrapper<SysComplaint>().eq("user_id", userId).orderByDesc("create_time"));
    }

    @Override
    public IPage<ComplaintDTO> adminList(Integer pageNum, Integer pageSize, String status) {
        // 由于需要远程调用UserService获取用户信息，这里不能直接在SQL中联表查询了
        // 或者保留联表查询（如果 user 表还在同一个库？不对，微服务拆分后 user 表在 user-service 库）
        // 假设已经分库，那么不能联表。
        // 这里需要重构：先查 Complaint，再填充 UserInfo
        
        Page<SysComplaint> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysComplaint> query = new QueryWrapper<>();
        
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null) query.eq("community_id", cid);
            else query.eq("id", -1L); // 无权限
        }
        
        if (status != null && !status.isEmpty()) {
            query.eq("status", status);
        }
        query.orderByDesc("create_time");
        
        IPage<SysComplaint> complaintPage = this.page(page, query);
        
        // 转换并填充用户信息
        return complaintPage.convert(c -> {
            ComplaintDTO dto = new ComplaintDTO();
            dto.setId(c.getId());
            dto.setUserId(c.getUserId());
            dto.setCommunityId(c.getCommunityId());
            dto.setType(c.getType());
            dto.setContent(c.getContent());
            dto.setImgs(c.getImgs());
            dto.setStatus(c.getStatus());
            dto.setResult(c.getResult());
            dto.setCreateTime(c.getCreateTime());
            dto.setHandleTime(c.getHandleTime());
            
            // 远程调用填充
            if (c.getUserId() != null) {
                try {
                    UserDTO user = userServiceClient.getUserById(c.getUserId());
                    if (user != null) {
                        dto.setUserName(user.getRealName());
                        dto.setUserPhone(user.getPhone());
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            return dto;
        });
    }

    @Override
    public boolean handle(Long id, String result) {
        SysComplaint c = this.getById(id);
        if (c == null) return false;
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid == null) {
                throw new RuntimeException("无权处理投诉");
            }
            if (c.getCommunityId() != null && !cid.equals(c.getCommunityId())) {
                throw new RuntimeException("无权处理其他社区投诉");
            }
        }
        
        c.setResult(result);
        c.setStatus("DONE");
        c.setHandleTime(LocalDateTime.now());
        return this.updateById(c);
    }
}
