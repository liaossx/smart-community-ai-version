package com.lsx.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.system.entity.SysOperLog;
import com.lsx.system.service.SysOperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/monitor/operlog")
@Tag(name = "系统操作日志")
public class SysOperLogController {

    @Resource
    private SysOperLogService operLogService;

    @GetMapping("/list")
    @Operation(summary = "查询操作日志列表")
    public Result<Page<SysOperLog>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) String title,
                                         @RequestParam(required = false) String operName,
                                         @RequestParam(required = false) Integer status) {
        Page<SysOperLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(title), SysOperLog::getTitle, title)
                .like(StringUtils.isNotBlank(operName), SysOperLog::getOperName, operName)
                .eq(status != null, SysOperLog::getStatus, status)
                .orderByDesc(SysOperLog::getOperTime);
        
        return Result.success(operLogService.page(page, wrapper));
    }

    @PostMapping
    @Operation(summary = "保存操作日志")
    public Result<Boolean> save(@RequestBody SysOperLog operLog) {
        return Result.success(operLogService.save(operLog));
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清空操作日志")
    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    public Result<String> clean() {
        operLogService.cleanOperLog();
        return Result.success("清空成功");
    }
}
