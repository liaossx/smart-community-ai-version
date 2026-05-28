package com.lsx.community.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.community.activity.entity.SysActivity;
import com.lsx.community.activity.service.ActivityService;
import com.lsx.community.activity.dto.SignupRecordDTO;
import com.lsx.core.common.Result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity")
@Tag(name = "社区活动接口")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/list")
    @Operation(summary = "活动列表")
    public Result<IPage<SysActivity>> list(@RequestParam(value = "status", required = false) String status,
                                           @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                           @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<SysActivity> page = activityService.list(status, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "活动详情")
    public Result<SysActivity> detail(@PathVariable("id") Long id) {
        SysActivity a = activityService.detail(id);
        if (a == null) return Result.fail("活动不存在");
        return Result.success(a);
    }

    @PostMapping("/join")
    @Operation(summary = "报名活动")
    public Result<Boolean> join(@RequestParam("activityId") Long activityId,
                                @RequestParam("userId") Long userId) {
        boolean ok = activityService.join(activityId, userId);
        return ok ? Result.success(true) : Result.fail("报名失败");
    }

    @PostMapping({"/publish", "/admin/publish"})
    @Operation(summary = "发布活动")
    @Log(title = "活动管理", businessType = BusinessType.INSERT)
    public Result<Long> publish(@RequestBody SysActivity body) {
        Long id = activityService.publish(body);
        return Result.success(id);
    }

    @DeleteMapping({"/{id}", "/admin/{id}"})
    @Operation(summary = "删除活动")
    @Log(title = "活动管理", businessType = BusinessType.DELETE)
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        boolean ok = activityService.deleteByIdWithCheck(id);
        return ok ? Result.success(true) : Result.fail("删除失败");
    }

    @GetMapping({"/signup/list", "/admin/signup/list"})
    @Operation(summary = "活动报名列表")
    public Result<IPage<SignupRecordDTO>> signupList(
            @RequestParam("activityId") Long activityId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<SignupRecordDTO> page = activityService.getSignupList(activityId, pageNum, pageSize);
        return Result.success(page);
    }

    @PutMapping({"", "/admin"})
    @Operation(summary = "修改活动")
    @Log(title = "活动管理", businessType = BusinessType.UPDATE)
    public Result<Boolean> update(@RequestBody SysActivity activity) {
        if (activity.getId() == null) {
            return Result.fail("活动ID不能为空");
        }
        Long id = activityService.publish(activity); 
        return id != null ? Result.success(true) : Result.fail("更新失败");
    }
}
