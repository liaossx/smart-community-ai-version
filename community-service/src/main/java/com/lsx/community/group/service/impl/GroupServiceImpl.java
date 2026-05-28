package com.lsx.community.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.community.client.UserServiceClient;
import com.lsx.community.dto.external.UserInfoDTO;
import com.lsx.community.group.dto.GroupCreateDTO;
import com.lsx.community.group.dto.GroupFinishDTO;
import com.lsx.community.group.dto.GroupJoinDTO;
import com.lsx.community.group.entity.GroupActivity;
import com.lsx.community.group.entity.GroupMember;
import com.lsx.community.group.enums.GroupStatusEnum;
import com.lsx.community.group.mapper.GroupActivityMapper;
import com.lsx.community.group.mapper.GroupMemberMapper;
import com.lsx.community.group.service.GroupService;
import com.lsx.community.group.vo.GroupMemberVO;
import com.lsx.community.group.vo.GroupVO;
import com.lsx.core.common.Util.UserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl extends ServiceImpl<GroupActivityMapper, GroupActivity> implements GroupService {

    @Autowired
    private GroupMemberMapper memberMapper;

    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    @Transactional
    public Long createGroup(GroupCreateDTO dto, Long userId) {
        GroupActivity activity = new GroupActivity();
        BeanUtils.copyProperties(dto, activity);
        activity.setCommunityId(UserContext.getCommunityId());
        activity.setCreatorId(userId);
        activity.setStatus(GroupStatusEnum.ONGOING.getCode());
        activity.setCurrentCount(0);
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(activity);
        return activity.getId();
    }

    @Override
    public Page<GroupVO> listGroups(Long communityId, Integer pageNum, Integer pageSize) {
        Page<GroupActivity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GroupActivity> query = new LambdaQueryWrapper<>();
        query.eq(GroupActivity::getCommunityId, communityId)
                .orderByDesc(GroupActivity::getCreateTime);

        Page<GroupActivity> activityPage = baseMapper.selectPage(page, query);

        Page<GroupVO> result = new Page<>(pageNum, pageSize);
        result.setTotal(activityPage.getTotal());

        List<GroupVO> vOs = activityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        result.setRecords(vOs);
        return result;
    }

    @Override
    public GroupVO getGroupDetail(Long activityId) {
        GroupActivity activity = baseMapper.selectById(activityId);
        if (activity == null) return null;
        return convertToVO(activity);
    }

    @Override
    @Transactional
    public Boolean joinGroup(GroupJoinDTO dto, Long userId) {
        GroupActivity activity = baseMapper.selectById(dto.getActivityId());
        if (activity == null) throw new RuntimeException("团购活动不存在");

        if (!GroupStatusEnum.ONGOING.getCode().equals(activity.getStatus())) {
            throw new RuntimeException("团购活动已结束或已取消");
        }

        if (activity.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("团购活动已到期");
        }

        GroupMember member = new GroupMember();
        member.setActivityId(dto.getActivityId());
        member.setUserId(userId);
        member.setBuyCount(dto.getBuyCount());
        member.setCreateTime(LocalDateTime.now());
        memberMapper.insert(member);

        activity.setCurrentCount(activity.getCurrentCount() + dto.getBuyCount());
        baseMapper.updateById(activity);

        return true;
    }

    @Override
    public List<GroupMemberVO> getGroupMembers(Long activityId) {
        LambdaQueryWrapper<GroupMember> query = new LambdaQueryWrapper<>();
        query.eq(GroupMember::getActivityId, activityId).orderByDesc(GroupMember::getCreateTime);
        List<GroupMember> members = memberMapper.selectList(query);

        if (members.isEmpty()) return Collections.emptyList();

        return members.stream().map(member -> {
            GroupMemberVO vo = new GroupMemberVO();
            vo.setUserId(member.getUserId());
            vo.setBuyCount(member.getBuyCount());
            vo.setCreateTime(member.getCreateTime());

            UserInfoDTO userInfo = userServiceClient.getUserById(member.getUserId());
            if (userInfo != null) {
                vo.setUserName(userInfo.getName());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Boolean finishGroup(GroupFinishDTO dto, Long userId) {
        GroupActivity activity = baseMapper.selectById(dto.getActivityId());
        if (activity == null) return false;

        if (!activity.getCreatorId().equals(userId)) {
            throw new RuntimeException("只有发起人可以结束团购");
        }

        activity.setStatus(dto.getStatus());
        activity.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(activity);
        return true;
    }

    private GroupVO convertToVO(GroupActivity activity) {
        GroupVO vo = new GroupVO();
        BeanUtils.copyProperties(activity, vo);
        if (activity.getImages() != null && !activity.getImages().isEmpty()) {
            vo.setImages(Arrays.asList(activity.getImages().split(",")));
        }

        UserInfoDTO userInfo = userServiceClient.getUserById(activity.getCreatorId());
        if (userInfo != null) {
            vo.setCreatorName(userInfo.getName());
        }

        return vo;
    }
}
