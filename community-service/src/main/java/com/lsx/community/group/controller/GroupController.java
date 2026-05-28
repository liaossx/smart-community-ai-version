package com.lsx.community.group.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.community.group.dto.GroupCreateDTO;
import com.lsx.community.group.dto.GroupFinishDTO;
import com.lsx.community.group.dto.GroupJoinDTO;
import com.lsx.community.group.service.GroupService;
import com.lsx.community.group.vo.GroupMemberVO;
import com.lsx.community.group.vo.GroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group")
@Tag(name = "社区团购接口")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping
    @Operation(summary = "发起团购")
    public Result<Long> create(@RequestBody GroupCreateDTO dto, @RequestParam("userId") Long userId) {
        return Result.success(groupService.createGroup(dto, userId));
    }

    @GetMapping("/list")
    @Operation(summary = "团购列表")
    public Result<Page<GroupVO>> list(@RequestParam("communityId") Long communityId,
                                      @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                      @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(groupService.listGroups(communityId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "团购详情")
    public Result<GroupVO> detail(@PathVariable("id") Long id) {
        return Result.success(groupService.getGroupDetail(id));
    }

    @PostMapping("/join")
    @Operation(summary = "参加团购")
    public Result<Boolean> join(@RequestBody GroupJoinDTO dto, @RequestParam("userId") Long userId) {
        return Result.success(groupService.joinGroup(dto, userId));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "团购成员列表")
    public Result<List<GroupMemberVO>> getMembers(@PathVariable("id") Long id) {
        return Result.success(groupService.getGroupMembers(id));
    }

    @PostMapping("/finish")
    @Operation(summary = "结束团购")
    public Result<Boolean> finish(@RequestBody GroupFinishDTO dto, @RequestParam("userId") Long userId) {
        return Result.success(groupService.finishGroup(dto, userId));
    }
}
