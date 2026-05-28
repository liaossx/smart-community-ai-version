package com.lsx.community.group.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.community.group.dto.GroupCreateDTO;
import com.lsx.community.group.dto.GroupFinishDTO;
import com.lsx.community.group.dto.GroupJoinDTO;
import com.lsx.community.group.entity.GroupActivity;
import com.lsx.community.group.vo.GroupMemberVO;
import com.lsx.community.group.vo.GroupVO;

import java.util.List;

public interface GroupService extends IService<GroupActivity> {
    Long createGroup(GroupCreateDTO dto, Long userId);
    Page<GroupVO> listGroups(Long communityId, Integer pageNum, Integer pageSize);
    GroupVO getGroupDetail(Long activityId);
    Boolean joinGroup(GroupJoinDTO dto, Long userId);
    List<GroupMemberVO> getGroupMembers(Long activityId);
    Boolean finishGroup(GroupFinishDTO dto, Long userId);
}
