package com.lsx.workorder.repair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.workorder.repair.dto.WorkOrderAssignDTO;
import com.lsx.workorder.repair.dto.WorkOrderCompleteDTO;
import com.lsx.workorder.repair.entity.WorkOrder;
import com.lsx.workorder.repair.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 工单管理接口
 */
@RestController
@RequestMapping("/api/workorder")
@Tag(name = "工单管理接口", description = "工单分配、执行、状态更新")
public class WorkOrderController {

    @Resource
    private WorkOrderService workOrderService;

    @Operation(summary = "分配工单", description = "管理员将工单分配给维修员")
    @PostMapping("/admin/assign")
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    public Result<Boolean> assignOrder(@RequestBody WorkOrderAssignDTO dto) {
        if (dto == null || dto.getOrderId() == null || dto.getWorkerId() == null) {
            return Result.fail("参数错误");
        }
        if (dto.getPriority() != null && (dto.getPriority() < 1 || dto.getPriority() > 4)) {
            return Result.fail("参数错误");
        }
        try {
            boolean success = workOrderService.assignToWorker(
                    dto.getOrderId(),
                    dto.getWorkerId(),
                    dto.getWorkerName(),
                    dto.getWorkerPhone(),
                    dto.getPriority()
            );
            return success ? Result.success(true) : Result.fail("分配失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "开始处理工单", description = "维修员开始处理工单")
    @PostMapping("/worker/start")
    @Log(title = "工单执行", businessType = BusinessType.UPDATE)
    public Result<Boolean> startProcess(@RequestParam Long orderId) {
        boolean success = workOrderService.startProcess(orderId);
        return success ? Result.success(true) : Result.fail("操作失败");
    }

    @Operation(summary = "完成工单", description = "维修员提交处理结果并完成工单")
    @PostMapping("/worker/complete")
    @Log(title = "工单执行", businessType = BusinessType.UPDATE)
    public Result<Boolean> completeOrder(@RequestBody WorkOrderCompleteDTO dto) {
        boolean success = workOrderService.completeWorkOrder(dto.getOrderId(), dto.getResult(), dto.getImages());
        return success ? Result.success(true) : Result.fail("提交失败");
    }

    @Operation(summary = "取消工单", description = "管理员取消工单")
    @PostMapping("/admin/cancel")
    @Log(title = "工单管理", businessType = BusinessType.UPDATE)
    public Result<Boolean> cancelOrder(@RequestParam Long orderId) {
        boolean success = workOrderService.cancelWorkOrder(orderId);
        return success ? Result.success(true) : Result.fail("取消失败");
    }

    @Operation(summary = "分页查询工单", description = "根据状态和关键词查询工单")
    @GetMapping("/list")
    public Result<IPage<WorkOrder>> getWorkOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        IPage<WorkOrder> workOrders = workOrderService.getWorkOrders(pageNum, pageSize, status, keyword);
        return Result.success(workOrders);
    }
}
