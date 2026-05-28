package com.lsx.property.complaint.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.property.complaint.entity.SysComplaint;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.property.complaint.dto.ComplaintDTO;
import com.lsx.property.complaint.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaint")
@Tag(name = "投诉建议接口")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @PostMapping("/submit")
    @Operation(summary = "提交投诉")
    public Result<Long> submit(@RequestBody SysComplaint body) {
        Long id = complaintService.submit(body);
        return Result.success(id);
    }

    @GetMapping("/my")
    @Operation(summary = "我的投诉")
    public Result<IPage<SysComplaint>> my(@RequestParam("userId") Long userId,
                                          @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<SysComplaint> page = complaintService.my(userId, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping({"/list", "/admin/list"})
    @Operation(summary = "管理员查询列表")
    public Result<IPage<ComplaintDTO>> list(@RequestParam(value = "status", required = false) String status,
                                            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<ComplaintDTO> page = complaintService.adminList(pageNum, pageSize, status);
        return Result.success(page);
    }

    @PutMapping({"/handle", "/admin/handle"})
    @Operation(summary = "处理投诉")
    @Log(title = "投诉建议", businessType = BusinessType.UPDATE)
    public Result<Boolean> handle(@RequestParam("id") Long id, @RequestParam("result") String result) {
        boolean ok = complaintService.handle(id, result);
        return ok ? Result.success(true) : Result.fail("处理失败");
    }
}
