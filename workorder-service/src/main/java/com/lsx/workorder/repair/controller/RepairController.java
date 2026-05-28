package com.lsx.workorder.repair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.workorder.repair.dto.BatchUpdateStatusDTO;
import com.lsx.workorder.repair.dto.RepairDto;
import com.lsx.workorder.repair.service.RepairService;
import com.lsx.workorder.repair.vo.RepairResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/repair")
@Tag(name = "报修管理接口", description = "报修提交、查询、状态更新")
public class RepairController {

    @Resource
    private RepairService repairService;

    @Operation(summary = "提交报修申请", description = "用户提交报修申请")
    @PostMapping("/submit")
    public Result<Boolean> submitRepair(
            @Parameter(description = "报修信息DTO")
            @RequestBody RepairDto repairDto) {
        boolean success = repairService.submitRepair(repairDto);
        if (success) {
            return Result.success(true);
        } else {
            return Result.fail("提交失败");
        }
    }

    @Operation(summary = "更新报修状态(管理员)", description = "管理员更新报修单状态")
    @PostMapping("/admin/updateStatus")
    @Log(title = "报修管理", businessType = BusinessType.UPDATE)
    public Result<Boolean> updateStatus(
            @Parameter(description = "报修ID", required = true) @RequestParam Long repairId,
            @Parameter(description = "状态: pending/processing/completed/cancelled", required = true) @RequestParam String status,
            @Parameter(description = "处理备注") @RequestParam(required = false) String remark) {
        try {
            boolean success = repairService.updateRepairStatus(repairId, status, remark);
            return success ? Result.success(true) : Result.fail("更新状态失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "查询我的报修", description = "查询当前登录用户的报修记录")
    @GetMapping("/user/my")
    public Result<IPage<RepairResult>> getMyRepairs(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<RepairResult> resultPage = repairService.getMyRepairs(userId, pageNum, pageSize);
        return Result.success(resultPage);
    }

    @GetMapping("/admin/all")
    @Operation(summary = "查询所有报修(管理员)", description = "管理员查询所有报修记录")
    public Result<IPage<RepairResult>> getAllRepairs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        IPage<RepairResult> resultPage = repairService.getAllRepairs(pageNum, pageSize, status, keyword);
        return Result.success(resultPage);
    }

    @Operation(summary = "查询指定用户报修(管理员)", description = "管理员查询指定用户的报修记录")
    @GetMapping("/admin/user")
    public Result<IPage<RepairResult>> getUserRepairs(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<RepairResult> resultPage = repairService.getUserRepairs(userId, pageNum, pageSize);
        return Result.success(resultPage);
    }

    @PostMapping("/admin/batchUpdateStatus")
    @Operation(summary = "批量更新状态", description = "管理员批量更新报修状态")
    @Log(title = "报修管理", businessType = BusinessType.UPDATE)
    public Result<Boolean> batchUpdateStatus(@RequestBody BatchUpdateStatusDTO dto) {
        boolean success = repairService.batchUpdateStatus(dto.getRepairIds(), dto.getStatus(), dto.getRemark());
        return success ? Result.success(true) : Result.fail("批量更新失败");
    }

    @GetMapping("/admin/export")
    @Operation(summary = "导出报修记录", description = "导出报修记录为CSV")
    public void exportRepairs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) {
        repairService.exportRepairs(status, keyword, response);
    }
}
