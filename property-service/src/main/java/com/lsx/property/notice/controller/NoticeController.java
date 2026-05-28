package com.lsx.property.notice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.annotation.Log;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.common.enums.BusinessType;
import com.lsx.property.notice.dto.BatchNoticeExpireDTO;
import com.lsx.property.notice.dto.ExpiringNoticeDTO;
import com.lsx.property.notice.dto.NoticeCreateDTO;
import com.lsx.property.notice.dto.NoticeDTO;
import com.lsx.property.notice.dto.NoticeReadStatDTO;
import com.lsx.property.notice.support.ConfigHelper;
import com.lsx.property.notice.entity.SysNotice;
import com.lsx.property.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notice")
@Tag(name = "通知公告接口", description = "通知公告的发布、查询、已读标记")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;
    @Autowired
    private ConfigHelper configHelper;

    @Value("${internal.token:}")
    private String internalToken;

    @PostMapping
    @Operation(summary = "发布通知", description = "管理员发布新通知")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    public Result<Long> createNotice(@RequestBody NoticeCreateDTO dto, @RequestParam("userId") Long userId) {
        Result<?> check = validateAndNormalize(dto);
        if (check != null) {
            return (Result<Long>) check;
        }
        return Result.success(noticeService.createNotice(dto, userId));
    }

    @PostMapping("/inner")
    @Operation(summary = "内部发布通知", description = "内部服务调用创建通知")
    public Result<Long> createNoticeInner(@RequestBody NoticeCreateDTO dto,
                                          @RequestParam("userId") Long userId,
                                          @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (internalToken != null && !internalToken.isEmpty() && !internalToken.equals(token)) {
            return Result.fail("无权访问");
        }
        Result<?> check = validateAndNormalize(dto);
        if (check != null) {
            return (Result<Long>) check;
        }
        return Result.success(noticeService.createNotice(dto, userId));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数", description = "获取当前用户的未读通知数量")
    public Result<Integer> getUnreadCount(@RequestParam("userId") Long userId) {
        String role = UserContext.getRole();
        boolean isOwner = role != null && role.toUpperCase().contains("OWNER");
        if (isOwner) {
            Long currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                return Result.fail("Token无效或已过期");
            }
            userId = currentUserId;
        }
        return Result.success(noticeService.getUnreadCount(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知", description = "管理员删除通知")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    public Result<Boolean> deleteNotice(@PathVariable("id") Long id) {
        return Result.success(noticeService.deleteNotice(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改通知", description = "管理员修改通知内容")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    public Result<Boolean> updateNotice(@PathVariable("id") Long id, @RequestBody NoticeCreateDTO dto) {
        Result<?> check = validateAndNormalize(dto);
        if (check != null) {
            return (Result<Boolean>) check;
        }
        return Result.success(noticeService.updateNotice(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "通知详情", description = "获取通知详情")
    public Result<SysNotice> getNotice(@PathVariable("id") Long id) {
        return Result.success(noticeService.getNoticeById(id));
    }

    @GetMapping({"/list", "/admin/list"})
    @Operation(summary = "通知列表", description = "分页查询通知列表")
    public Result<?> listNotices(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        pageSize = clampPageSize(pageSize);
        String role = UserContext.getRole();
        boolean isOwner = role != null && role.toUpperCase().contains("OWNER");
        if (isOwner) {
            Long currentUserId = UserContext.getCurrentUserId();
            Long effectiveUserId = currentUserId != null ? currentUserId : userId;
            if (effectiveUserId == null) {
                return Result.success(new Page<>(pageNum, pageSize));
            }
            return Result.success(noticeService.getUserNotices(effectiveUserId, pageNum, pageSize));
        }
        return Result.success(noticeService.listNotices(title, status, pageNum, pageSize));
    }

    @GetMapping("/user/list")
    @Operation(summary = "用户通知列表", description = "用户查询可见的通知列表")
    public Result<Page<NoticeDTO>> userList(
            @RequestParam("userId") Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        pageSize = clampPageSize(pageSize);
        String role = UserContext.getRole();
        boolean isOwner = role != null && role.toUpperCase().contains("OWNER");
        if (isOwner) {
            Long currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                return Result.fail("Token无效或已过期");
            }
            userId = currentUserId;
        }
        return Result.success(noticeService.getUserNotices(userId, pageNum, pageSize));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记已读", description = "用户标记通知为已读")
    public Result<Boolean> readNotice(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        String role = UserContext.getRole();
        boolean isOwner = role != null && role.toUpperCase().contains("OWNER");
        if (isOwner) {
            Long currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                return Result.fail("Token无效或已过期");
            }
            userId = currentUserId;
        }
        return Result.success(noticeService.readNotice(id, userId));
    }
    
    @GetMapping("/expiring")
    @Operation(summary = "即将过期通知", description = "查询即将过期的通知")
    public Result<List<ExpiringNoticeDTO>> getExpiringNotices(
            @Parameter(description = "过期天数阈值") @RequestParam(defaultValue = "3") Integer days) {
        return Result.success(noticeService.getExpiringNotices(days));
    }
    
    @PostMapping("/expire/batch")
    @Operation(summary = "批量设置过期", description = "管理员批量设置通知过期时间")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    public Result<Boolean> batchExpire(@RequestBody BatchNoticeExpireDTO dto) {
        return Result.success(noticeService.batchExpireNotices(dto));
    }

    @GetMapping("/{id}/read-stat")
    @Operation(summary = "通知已读统计", description = "管理员查询指定通知的已读/未读统计")
    public Result<NoticeReadStatDTO> getReadStat(@PathVariable("id") Long id) {
        NoticeReadStatDTO stat = noticeService.getReadStat(id);
        if (stat == null) {
            return Result.fail("通知不存在");
        }
        return Result.success(stat);
    }

    private Integer clampPageSize(Integer pageSize) {
        int max = configHelper.getInt("notice.page.size.max", 50);
        if (max <= 0) {
            return pageSize;
        }
        if (pageSize == null) {
            return Math.min(10, max);
        }
        return Math.min(pageSize, max);
    }

    private Result<?> validateAndNormalize(NoticeCreateDTO dto) {
        if (dto == null) {
            return Result.fail("参数不能为空");
        }

        int titleMax = configHelper.getInt("notice.title.length.max", 100);
        if (titleMax > 0 && dto.getTitle() != null && dto.getTitle().length() > titleMax) {
            return Result.fail("公告标题长度超过限制");
        }

        int contentMax = configHelper.getInt("notice.content.length.max", 20000);
        if (contentMax > 0 && dto.getContent() != null && dto.getContent().length() > contentMax) {
            return Result.fail("公告内容长度超过限制");
        }

        boolean topAllowed = configHelper.getBool("notice.top.allowed", true);
        if (!topAllowed && Boolean.TRUE.equals(dto.getTopFlag())) {
            dto.setTopFlag(false);
        }

        boolean expireRequired = configHelper.getBool("notice.expire.required.published", false);
        if (expireRequired && "PUBLISHED".equalsIgnoreCase(dto.getPublishStatus()) && dto.getExpireTime() == null) {
            int defaultDays = configHelper.getInt("notice.default.expire.days", 0);
            if (defaultDays <= 0) {
                return Result.fail("发布公告必须设置过期时间");
            }
            dto.setExpireTime(LocalDateTime.now().plusDays(defaultDays));
        }

        return null;
    }
}
