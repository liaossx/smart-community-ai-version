package com.lsx.house.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.house.dto.HouseBindApproveDTO;
import com.lsx.house.dto.HouseBindRejectDTO;
import com.lsx.house.dto.HouseBindRequestDTO;
import com.lsx.house.dto.HouseBindRequestDetailDTO;
import com.lsx.house.entity.HouseBindRequest;
import com.lsx.house.service.HouseBindRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/house")
@Tag(name = "管理员-房屋绑定审核接口")
public class AdminHouseBindController {

    @Resource
    private HouseBindRequestService houseBindRequestService;

    @Operation(summary = "获取房屋绑定申请列表（分页）")
    @GetMapping("/bind-requests")
    public Result<Page<HouseBindRequestDTO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long communityId
    ) {
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        if (role == null || !"super_admin".equalsIgnoreCase(role)) {
            communityId = currentCommunityId;
        }
        Page<HouseBindRequest> page = houseBindRequestService.pageRequests(pageNum, pageSize, keyword, status, communityId);
        Page<HouseBindRequestDTO> out = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<HouseBindRequestDTO> records = new ArrayList<>();
        for (HouseBindRequest r : page.getRecords()) {
            HouseBindRequestDTO dto = new HouseBindRequestDTO();
            dto.setId(r.getId());
            dto.setUserId(r.getUserId());
            dto.setUsername(r.getUsername());
            dto.setRealName(r.getRealName());
            dto.setPhone(r.getPhone());
            dto.setCommunityName(r.getCommunityName());
            dto.setBuildingNo(r.getBuildingNo());
            dto.setHouseNo(r.getHouseNo());
            dto.setIdentityType(r.getIdentityType());
            dto.setStatus(r.getStatus());
            dto.setApplyTime(r.getApplyTime());
            dto.setRejectReason(r.getRejectReason());
            records.add(dto);
        }
        out.setRecords(records);
        return Result.success(out);
    }

    @Operation(summary = "获取申请详情")
    @GetMapping("/bind-requests/{id}")
    public Result<HouseBindRequestDetailDTO> detail(@PathVariable("id") Long id) {
        HouseBindRequest r = houseBindRequestService.getById(id);
        if (r == null) {
            return Result.fail("绑定申请不存在");
        }
        HouseBindRequestDetailDTO dto = new HouseBindRequestDetailDTO();
        dto.setId(r.getId());
        dto.setUserId(r.getUserId());
        dto.setUsername(r.getUsername());
        dto.setRealName(r.getRealName());
        dto.setPhone(r.getPhone());
        dto.setHouseId(r.getHouseId());
        dto.setCommunityId(r.getCommunityId());
        dto.setCommunityName(r.getCommunityName());
        dto.setBuildingNo(r.getBuildingNo());
        dto.setHouseNo(r.getHouseNo());
        dto.setIdentityType(r.getIdentityType());
        dto.setStatus(r.getStatus());
        dto.setApplyTime(r.getApplyTime());
        dto.setApproveTime(r.getApproveTime());
        dto.setApproveBy(r.getApproveBy());
        dto.setRejectReason(r.getRejectReason());
        return Result.success(dto);
    }

    @Operation(summary = "审核通过")
    @PutMapping("/bind-requests/{id}/approve")
    public Result<Boolean> approve(@PathVariable("id") Long id, @RequestBody(required = false) HouseBindApproveDTO body) {
        try {
            Long adminId = UserContext.getCurrentUserId();
            if (adminId == null) {
                return Result.fail("未登录");
            }
            String identityType = body != null ? body.getIdentityType() : null;
            return Result.success(houseBindRequestService.approve(id, identityType, adminId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "驳回")
    @PutMapping("/bind-requests/{id}/reject")
    public Result<Boolean> reject(@PathVariable("id") Long id, @RequestBody HouseBindRejectDTO body) {
        try {
            Long adminId = UserContext.getCurrentUserId();
            if (adminId == null) {
                return Result.fail("未登录");
            }
            String reason = body != null ? body.getReason() : null;
            return Result.success(houseBindRequestService.reject(id, reason, adminId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
