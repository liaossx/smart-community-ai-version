package com.lsx.community.topic.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.community.topic.dto.TopicAuditDTO;
import com.lsx.community.topic.dto.TopicCommentDTO;
import com.lsx.community.topic.dto.TopicCreateDTO;
import com.lsx.community.topic.service.TopicService;
import com.lsx.community.topic.vo.TopicCommentVO;
import com.lsx.community.topic.vo.TopicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topic")
@Tag(name = "社区话题接口")
public class TopicController {

    @Autowired
    private TopicService topicService;

    @PostMapping
    @Operation(summary = "发布话题")
    public Result<Long> create(@RequestBody TopicCreateDTO dto, @RequestParam("userId") Long userId) {
        return Result.success(topicService.createTopic(dto, userId));
    }

    @GetMapping("/list")
    @Operation(summary = "话题列表")
    public Result<Page<TopicVO>> list(@RequestParam(value = "communityId", required = false) Long communityId,
                                      @RequestParam(value = "userId", required = false) Long userId,
                                      @RequestParam(value = "status", required = false) String status,
                                      @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                      @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Long effectiveCommunityId = communityId != null ? communityId : UserContext.getCommunityId();
        if (effectiveCommunityId == null) {
            return Result.fail("缺少 communityId");
        }

        Long effectiveUserId = userId != null ? userId : UserContext.getCurrentUserId();
        String effectiveStatus = status;
        if (effectiveStatus != null) {
            effectiveStatus = effectiveStatus.trim();
            if (effectiveStatus.endsWith(",")) {
                effectiveStatus = effectiveStatus.substring(0, effectiveStatus.length() - 1).trim();
            }
        }

        return Result.success(topicService.listTopics(effectiveCommunityId, effectiveUserId, effectiveStatus, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "话题详情")
    public Result<TopicVO> detail(@PathVariable("id") Long topicId, @RequestParam(value = "userId", required = false) Long userId) {
        return Result.success(topicService.getTopicDetail(topicId, userId));
    }

    @RequestMapping(value = "/{id}/like", method = {RequestMethod.POST, RequestMethod.PUT})
    @Operation(summary = "点赞/取消点赞")
    public Result<Boolean> like(@PathVariable("id") Long topicId, @RequestParam("userId") Long userId) {
        return Result.success(topicService.likeTopic(topicId, userId));
    }

    @PostMapping(value = {"/comment", "/{id}/comment"})
    @Operation(summary = "发表评论")
    public Result<Long> comment(@RequestBody TopicCommentDTO dto, @RequestParam(value = "userId", required = false) Long userId, @PathVariable(value = "id", required = false) Long topicId) {
        if (topicId != null && dto.getTopicId() == null) {
            dto.setTopicId(topicId);
        }
        // 优先从 query 参数取，没有则尝试从 dto body 取，最后从上下文取
        Long finalUserId = userId;
        if (finalUserId == null) {
             // 假设 DTO 里没有 userId 字段（通常没有），则依靠 UserContext
             finalUserId = UserContext.getCurrentUserId();
        }
        
        if (finalUserId == null) {
             return Result.fail("无法获取当前用户ID");
        }
        
        return Result.success(topicService.commentTopic(dto, finalUserId));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "获取评论列表")
    public Result<List<TopicCommentVO>> getComments(@PathVariable("id") Long topicId) {
        return Result.success(topicService.getComments(topicId));
    }

    @PostMapping("/audit")
    @Operation(summary = "审核话题")
    public Result<Boolean> audit(@RequestBody TopicAuditDTO dto) {
        return Result.success(topicService.auditTopic(dto));
    }
}
