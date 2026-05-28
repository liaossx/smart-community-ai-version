package com.lsx.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.user.entity.Community;
import com.lsx.user.entity.User;
import com.lsx.user.entity.UserRegisterRequest;
import com.lsx.user.mapper.CommunityMapper;
import com.lsx.user.mapper.UserMapper;
import com.lsx.user.mapper.UserRegisterRequestMapper;
import com.lsx.user.service.UserRegisterRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class UserRegisterRequestServiceImpl extends ServiceImpl<UserRegisterRequestMapper, UserRegisterRequest>
        implements UserRegisterRequestService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private CommunityMapper communityMapper;

    @Override
    public Page<UserRegisterRequest> pageRequests(Integer pageNum, Integer pageSize, String keyword, String status, String role) {
        Page<UserRegisterRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserRegisterRequest> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(UserRegisterRequest::getUsername, keyword)
                    .or()
                    .like(UserRegisterRequest::getPhone, keyword)
                    .or()
                    .like(UserRegisterRequest::getRealName, keyword));
        }
        if (StringUtils.hasText(status)) {
            String st = status;
            if ("ACTIVE".equalsIgnoreCase(st)) {
                st = "APPROVED";
            }
            w.eq(UserRegisterRequest::getStatus, st);
        }
        if (StringUtils.hasText(role)) {
            w.eq(UserRegisterRequest::getRole, role);
        }
        w.orderByDesc(UserRegisterRequest::getApplyTime);
        return this.page(page, w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long id, String role, Long communityId, Long adminId) {
        UserRegisterRequest req = this.getById(id);
        if (req == null) {
            throw new RuntimeException("注册申请不存在");
        }
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("注册申请不存在或状态不是待审核");
        }

        String finalRole = StringUtils.hasText(role) ? role : req.getRole();
        if ("superadmin".equalsIgnoreCase(finalRole)) {
            finalRole = "super_admin";
        }
        if (!StringUtils.hasText(finalRole)
                || (!"owner".equals(finalRole)
                && !"admin".equals(finalRole)
                && !"super_admin".equals(finalRole)
                && !"worker".equals(finalRole))) {
            throw new RuntimeException("角色必须是 owner、admin、super_admin 或 worker");
        }

        Long finalCommunityId = resolveApprovedCommunityId(finalRole, communityId, req.getCommunityId());

        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        if (StringUtils.hasText(req.getPhone())) {
            User phoneUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
            if (phoneUser != null) {
                throw new RuntimeException("手机号已存在");
            }
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        user.setRole(finalRole);
        user.setCommunityId(finalCommunityId);
        user.setStatus(1);
        user.setBalance(java.math.BigDecimal.ZERO);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        if (userMapper.insert(user) <= 0) {
            throw new RuntimeException("创建用户失败");
        }

        req.setRole(finalRole);
        req.setCommunityId(finalCommunityId);
        req.setStatus("APPROVED");
        req.setApproveTime(LocalDateTime.now());
        req.setApproveBy(adminId);
        req.setRejectReason(null);
        return this.updateById(req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long id, String reason, Long adminId) {
        UserRegisterRequest req = this.getById(id);
        if (req == null) {
            throw new RuntimeException("注册申请不存在");
        }
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("注册申请不存在或状态不是待审核");
        }
        req.setStatus("REJECTED");
        req.setRejectReason(reason);
        req.setApproveTime(LocalDateTime.now());
        req.setApproveBy(adminId);
        return this.updateById(req);
    }

    private Long resolveApprovedCommunityId(String role, Long requestCommunityId, Long originalCommunityId) {
        if ("admin".equalsIgnoreCase(role)) {
            if (requestCommunityId == null) {
                throw new RuntimeException("管理员账号必须选择负责社区");
            }
            validateCommunityExists(requestCommunityId);
            return requestCommunityId;
        }
        if ("worker".equalsIgnoreCase(role)) {
            if (requestCommunityId == null) {
                throw new RuntimeException("工作人员账号必须选择所属社区");
            }
            validateCommunityExists(requestCommunityId);
            return requestCommunityId;
        }
        return requestCommunityId != null ? requestCommunityId : originalCommunityId;
    }

    private void validateCommunityExists(Long communityId) {
        Community community = communityMapper.selectById(communityId);
        if (community == null) {
            throw new RuntimeException("所选社区不存在");
        }
    }
}
