package com.lsx.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.user.dto.AdminUpdateUserDTO;
import com.lsx.user.dto.RegisterRequestApproveDTO;
import com.lsx.user.dto.RegisterRequestDTO;
import com.lsx.user.dto.RegisterRequestDetailDTO;
import com.lsx.user.dto.RegisterRequestRejectDTO;
import com.lsx.user.dto.UserDTO;
import com.lsx.user.dto.UserDetailDTO;
import com.lsx.user.entity.UserRegisterRequest;
import com.lsx.user.service.UserRegisterRequestService;
import com.lsx.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/user")
@Tag(name = "管理员-用户管理接口")
public class AdminUserController {

    @Resource
    private UserService userService;

    @Resource
    private UserRegisterRequestService userRegisterRequestService;

    @Operation(summary = "分页获取用户列表")
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role
    ) {
        Page<UserDTO> page = userService.getUserList(pageNum, pageSize, keyword, role);
        
        // 转换记录列表
        List<Map<String, Object>> records = new ArrayList<>();
        for (UserDTO dto : page.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", dto.getId()); // 统一使用 id
            item.put("userId", dto.getId()); // 兼容旧字段
            item.put("username", dto.getUsername());
            item.put("realName", dto.getRealName());
            item.put("phone", dto.getPhone());
            // 角色转换逻辑保留
            item.put("role", "owner".equalsIgnoreCase(dto.getRole()) ? "user" : dto.getRole());
            item.put("status", dto.getStatus());
            item.put("createTime", dto.getCreateTime());
            records.add(item);
        }

        // 构建标准分页对象
        Page<Map<String, Object>> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(records);
        
        return Result.success(resultPage);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{userId}")
    public Result<UserDetailDTO> getUserDetail(@PathVariable Long userId) {
        return Result.success(userService.getUserDetail(userId));
    }

    @Operation(summary = "管理员修改用户信息")
    @PutMapping("/update")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<String> updateUser(@RequestBody AdminUpdateUserDTO dto) {
        userService.adminUpdateUser(dto);
        Result<String> result = new Result<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(null);
        return result;
    }

    @Operation(summary = "修改用户状态(禁用/启用)")
    @PutMapping("/{userId}/status")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<String> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Integer status
    ) {
        userService.updateStatus(userId, status);
        Result<String> result = new Result<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(null);
        return result;
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{userId}/reset-password")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<String> resetPassword(@PathVariable Long userId,
                                        @RequestBody(required = false) Map<String, String> body) {
        String newPassword = body != null ? body.get("newPassword") : null;
        userService.resetPassword(userId, newPassword);
        Result<String> result = new Result<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(null);
        return result;
    }

    @Operation(summary = "获取注册申请列表（分页）")
    @GetMapping("/register-requests")
    public Result<Page<RegisterRequestDTO>> listRegisterRequests(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role
    ) {
        Page<UserRegisterRequest> page = userRegisterRequestService.pageRequests(pageNum, pageSize, keyword, status, role);
        Page<RegisterRequestDTO> out = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<RegisterRequestDTO> records = new ArrayList<>();
        for (UserRegisterRequest r : page.getRecords()) {
            RegisterRequestDTO dto = new RegisterRequestDTO();
            dto.setId(r.getId());
            dto.setUsername(r.getUsername());
            dto.setPhone(r.getPhone());
            dto.setRealName(r.getRealName());
            dto.setRole(r.getRole());
            dto.setStatus(r.getStatus());
            dto.setApplyTime(r.getApplyTime());
            records.add(dto);
        }
        out.setRecords(records);
        return Result.success(out);
    }

    @Operation(summary = "获取注册申请详情")
    @GetMapping("/register-requests/{id}")
    public Result<RegisterRequestDetailDTO> getRegisterRequest(@PathVariable("id") Long id) {
        UserRegisterRequest r = userRegisterRequestService.getById(id);
        if (r == null) {
            return Result.fail("注册申请不存在");
        }
        RegisterRequestDetailDTO dto = new RegisterRequestDetailDTO();
        dto.setId(r.getId());
        dto.setUsername(r.getUsername());
        dto.setPhone(r.getPhone());
        dto.setRealName(r.getRealName());
        dto.setRole(r.getRole());
        dto.setStatus(r.getStatus());
        dto.setCommunityId(r.getCommunityId());
        dto.setApplyTime(r.getApplyTime());
        dto.setApproveTime(r.getApproveTime());
        dto.setApproveBy(r.getApproveBy());
        dto.setRejectReason(r.getRejectReason());
        return Result.success(dto);
    }

    @Operation(summary = "审核通过")
    @PutMapping("/register-requests/{id}/approve")
    @Log(title = "注册审核", businessType = BusinessType.UPDATE)
    public Result<Boolean> approveRegisterRequest(@PathVariable("id") Long id, @RequestBody(required = false) RegisterRequestApproveDTO body) {
        try {
            Long adminId = UserContext.getCurrentUserId();
            if (adminId == null) {
                return Result.fail("未登录");
            }
            String role = body != null ? body.getRole() : null;
            Long communityId = body != null ? body.getCommunityId() : null;
            return Result.success(userRegisterRequestService.approve(id, role, communityId, adminId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "驳回")
    @PutMapping("/register-requests/{id}/reject")
    @Log(title = "注册审核", businessType = BusinessType.UPDATE)
    public Result<Boolean> rejectRegisterRequest(@PathVariable("id") Long id, @RequestBody RegisterRequestRejectDTO body) {
        try {
            Long adminId = UserContext.getCurrentUserId();
            if (adminId == null) {
                return Result.fail("未登录");
            }
            String reason = body != null ? body.getReason() : null;
            return Result.success(userRegisterRequestService.reject(id, reason, adminId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}

