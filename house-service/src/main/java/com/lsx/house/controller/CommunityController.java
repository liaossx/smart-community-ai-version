package com.lsx.house.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.house.entity.Community;
import com.lsx.house.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/house/community")
@Tag(name = "社区管理接口")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @GetMapping({"/list", "/admin/list"})
    @Operation(summary = "查询社区列表")
    public Result<IPage<Community>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) String name) {
        Page<Community> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Community> queryWrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            queryWrapper.like(Community::getName, name);
        }
        return Result.success(communityService.page(page, queryWrapper));
    }
    
    @GetMapping({"/all", "/admin/all"})
    @Operation(summary = "查询所有社区")
    public Result<List<Community>> all() {
        return Result.success(communityService.list());
    }

    @PostMapping({"", "/admin"})
    @Operation(summary = "新增社区")
    @Log(title = "社区管理", businessType = BusinessType.INSERT)
    public Result<Boolean> add(@RequestBody Community community) {
        return Result.success(communityService.save(community));
    }

    @PutMapping({"", "/admin"})
    @Operation(summary = "修改社区")
    @Log(title = "社区管理", businessType = BusinessType.UPDATE)
    public Result<Boolean> update(@RequestBody Community community) {
        return Result.success(communityService.updateById(community));
    }

    @DeleteMapping({"/{id}", "/admin/{id}"})
    @Operation(summary = "删除社区")
    @Log(title = "社区管理", businessType = BusinessType.DELETE)
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(communityService.removeById(id));
    }
    
    @GetMapping({"/{id}", "/admin/{id}"})
    @Operation(summary = "获取社区详情")
    public Result<Community> get(@PathVariable Long id) {
        return Result.success(communityService.getById(id));
    }
}
