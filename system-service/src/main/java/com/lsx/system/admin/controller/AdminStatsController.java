package com.lsx.system.admin.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.system.admin.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@Tag(name = "管理员-数据统计接口")
public class AdminStatsController {

    @Resource
    private AdminStatsService adminStatsService;

    @GetMapping("/overview")
    @Operation(summary = "获取首页统计概览")
    @SentinelResource(value = "admin-stats-overview", blockHandler = "handleOverviewBlock", fallback = "handleOverviewFallback")
    public Result<Map<String, Object>> getOverview(@RequestParam(value = "communityId", required = false) Long communityIdParam,
                                                   HttpServletResponse response) {
        String role = UserContext.getRole();
        Long tokenCommunityId = UserContext.getCommunityId();
        return Result.success(adminStatsService.getOverviewData(role, tokenCommunityId, communityIdParam));
    }

    public Result<Map<String, Object>> handleOverviewBlock(Long communityIdParam, HttpServletResponse response, BlockException ex) {
        if (response != null) {
            response.setStatus(429);
        }
        return Result.fail("请求过于频繁，请稍后再试");
    }

    public Result<Map<String, Object>> handleOverviewFallback(Long communityIdParam, HttpServletResponse response, Throwable ex) {
        if (response != null) {
            response.setStatus(503);
        }
        return Result.fail("服务繁忙，请稍后再试");
    }
}
