package com.lsx.property.visitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.property.visitor.entity.SysVisitor;
import com.lsx.property.visitor.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.property.visitor.dto.VisitorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/visitor")
@Tag(name = "访客管理接口")
public class VisitorController {

    private static final Logger log = LoggerFactory.getLogger(VisitorController.class);

    @Autowired
    private VisitorService visitorService;

    @PostMapping("/apply")
    @Operation(summary = "访客申请")
    public Result<Long> apply(@RequestBody SysVisitor body) {
        Long id = visitorService.apply(body);
        return Result.success(id);
    }

    @GetMapping("/my")
    @Operation(summary = "我的访客列表")
    public Result<IPage<SysVisitor>> my(@RequestParam("userId") Long userId,
                                        @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<SysVisitor> page = visitorService.myList(userId, pageNum, pageSize);
        return Result.success(page);
    }


    @GetMapping({"/list", "/admin/list"})
    @Operation(summary = "管理员查询列表")
    public Result<IPage<VisitorDTO>> adminList(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                               @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                               @RequestParam(value = "status", required = false) String status,
                                               @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            log.info("管理员查询访客列表: pageNum={}, pageSize={}, status={}, keyword={}", pageNum, pageSize, status, keyword);
            IPage<VisitorDTO> page = visitorService.adminList(pageNum, pageSize, status, keyword);
            log.info("查询成功: total={}", page.getTotal());
            return Result.success(page);
        } catch (Exception e) {
            log.error("管理员查询列表失败!", e);
            throw e;
        }
    }

    @PutMapping({"/audit", "/admin/audit"})
    @Operation(summary = "审核访客")
    @Log(title = "访客管理", businessType = BusinessType.UPDATE)
    public Result<Boolean> audit(@RequestParam("id") Long id,
                                 @RequestParam("status") String status,
                                 @RequestParam(value = "remark", required = false) String remark) {
        boolean ok = visitorService.audit(id, status, remark);
        return ok ? Result.success(true) : Result.fail("操作失败");
    }
}
