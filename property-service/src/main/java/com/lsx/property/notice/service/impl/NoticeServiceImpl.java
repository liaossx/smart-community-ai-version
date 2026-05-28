package com.lsx.property.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.client.HouseServiceClient;
import com.lsx.property.dto.external.HouseDTO;
import com.lsx.property.notice.dto.*;
import com.lsx.property.notice.entity.SysNotice;
import com.lsx.property.notice.entity.SysNoticeRead;
import com.lsx.property.notice.mapper.SysNoticeMapper;
import com.lsx.property.notice.mapper.SysNoticeReadMapper;
import com.lsx.property.notice.service.NoticeService;
import com.lsx.property.notice.support.ConfigHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements NoticeService {

    @Autowired
    private SysNoticeMapper noticeMapper;
    @Autowired
    private SysNoticeReadMapper noticeReadMapper;
    @Autowired
    private HouseServiceClient houseServiceClient;
    @Autowired
    private ConfigHelper configHelper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"noticeUserList", "noticeUnreadCount"}, allEntries = true)
    public Long createNotice(NoticeCreateDTO dto, Long userId) {
        SysNotice notice = new SysNotice();
        BeanUtils.copyProperties(dto, notice);
        notice.setTargetBuilding(dto.getBuildingNo());
        notice.setCreatorId(userId);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        
        // 权限处理
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();

        if (!StringUtils.hasText(role)) {
            if (dto.getCommunityId() != null) {
                notice.setCommunityId(dto.getCommunityId());
                if (!StringUtils.hasText(dto.getCommunityName())) {
                    String communityName = houseServiceClient.getCommunityNameById(dto.getCommunityId());
                    if (communityName != null) {
                        notice.setCommunityName(communityName);
                    }
                }
            }
        } else if (!"super_admin".equalsIgnoreCase(role)) {
            if (communityId == null) {
                throw new RuntimeException("普通管理员必须绑定社区才能发布通知");
            }
            notice.setCommunityId(communityId);
            String communityName = houseServiceClient.getCommunityNameById(communityId);
            if (communityName != null) {
                notice.setCommunityName(communityName);
            }
        } else {
            if (dto.getCommunityId() != null) {
                notice.setCommunityId(dto.getCommunityId());
                String communityName = houseServiceClient.getCommunityNameById(dto.getCommunityId());
                if (communityName != null) {
                    notice.setCommunityName(communityName);
                }
            }
        }

        if (notice.getPublishStatus() == null) {
            notice.setPublishStatus("DRAFT");
        }
        if ("PUBLISHED".equalsIgnoreCase(notice.getPublishStatus())) {
            notice.setPublishTime(LocalDateTime.now());
        }
        
        if (notice.getExpireTime() == null) {
            int days = configHelper.getInt("notice.default.expire.days", 0);
            if (days > 0) {
                notice.setExpireTime(LocalDateTime.now().plusDays(days));
            }
        }
        if (notice.getTopFlag() == null) {
            boolean top = configHelper.getBool("notice.default.top", false);
            notice.setTopFlag(top);
        }
        
        notice.setDeleted(0);
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Override
    public Page<SysNotice> listNotices(String title, String status, Integer pageNum, Integer pageSize) {
        Page<SysNotice> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (communityId != null) {
                wrapper.eq(SysNotice::getCommunityId, communityId);
            } else {
                // 未绑定社区的普通管理员，只能看空的或者全平台的（假设没有全平台）
                wrapper.eq(SysNotice::getId, -1L);
            }
        }
        
        if (StringUtils.hasText(title)) {
            wrapper.like(SysNotice::getTitle, title);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysNotice::getPublishStatus, status);
        }
        
        wrapper.eq(SysNotice::getDeleted, 0);
        wrapper.orderByDesc(SysNotice::getCreateTime);
        
        return noticeMapper.selectPage(page, wrapper);
    }

    @Override
    @Cacheable(cacheNames = "noticeUserList", key = "#userId + ':' + #pageNum + ':' + #pageSize")
    public Page<NoticeDTO> getUserNotices(Long userId, Integer pageNum, Integer pageSize) {
        // 1. 获取用户关联的房屋信息
        List<HouseDTO> userHouses = houseServiceClient.getHousesByUserId(userId);
        
        Set<String> communityNames = new HashSet<>();
        Set<String> buildingNos = new HashSet<>();
        
        if (userHouses != null) {
            for (HouseDTO h : userHouses) {
                if (h.getCommunityName() != null) communityNames.add(h.getCommunityName());
                if (h.getBuildingNo() != null) buildingNos.add(h.getBuildingNo());
            }
        }
        
        // 2. 构建查询条件
        Page<SysNotice> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysNotice> wrapper = new QueryWrapper<>();
        wrapper.eq("publish_status", "PUBLISHED")
                .eq("deleted", 0)
                .and(w -> w
                        .eq("target_type", "ALL")
                        .or(!communityNames.isEmpty(), c -> c.eq("target_type", "COMMUNITY").in("community_name", communityNames))
                        .or(!buildingNos.isEmpty(), b -> b.eq("target_type", "BUILDING").in("target_building", buildingNos))
                        .or(u -> u.eq("target_type", "USER").eq("target_user_id", userId))
                )
                // 排除已过期的
                .and(w -> w.isNull("expire_time").or().gt("expire_time", LocalDateTime.now()))
                .orderByDesc("top_flag", "publish_time");
        
        Page<SysNotice> noticePage = noticeMapper.selectPage(page, wrapper);
        
        // 3. 获取已读状态
        List<Long> noticeIds = noticePage.getRecords().stream().map(SysNotice::getId).collect(Collectors.toList());
        Set<Long> readIds = new HashSet<>();
        if (!noticeIds.isEmpty()) {
            List<SysNoticeRead> reads = noticeReadMapper.selectList(new QueryWrapper<SysNoticeRead>()
                    .eq("user_id", userId)
                    .in("notice_id", noticeIds)
                    .eq("status", "READ"));
            reads.forEach(r -> readIds.add(r.getNoticeId()));
        }
        
        // 4. 转换DTO
        return (Page<NoticeDTO>) noticePage.convert(n -> {
            NoticeDTO dto = new NoticeDTO();
            BeanUtils.copyProperties(n, dto);
            dto.setRead(readIds.contains(n.getId()));
            return dto;
        });
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"noticeUserList", "noticeUnreadCount"}, allEntries = true)
    public Boolean readNotice(Long noticeId, Long userId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || notice.getDeleted() == 1) {
            return false;
        }
        
        SysNoticeRead read = noticeReadMapper.selectOne(new QueryWrapper<SysNoticeRead>()
                .eq("notice_id", noticeId)
                .eq("user_id", userId));
        
        if (read == null) {
            read = new SysNoticeRead();
            read.setNoticeId(noticeId);
            read.setUserId(userId);
            read.setStatus("READ");
            read.setReadTime(LocalDateTime.now());
            read.setCreateTime(LocalDateTime.now());
            noticeReadMapper.insert(read);
        } else if (!"READ".equals(read.getStatus())) {
            read.setStatus("READ");
            read.setReadTime(LocalDateTime.now());
            noticeReadMapper.updateById(read);
        }
        return true;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"noticeUserList", "noticeUnreadCount"}, allEntries = true)
    public Boolean deleteNotice(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) return false;

        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (communityId == null) {
                throw new RuntimeException("无权删除通知");
            }
            if (notice.getCommunityId() != null && !communityId.equals(notice.getCommunityId())) {
                throw new RuntimeException("无权删除其他社区通知");
            }
        }

        notice.setDeleted(1);
        noticeMapper.updateById(notice);
        return true;
    }

    @Override
    public SysNotice getNoticeById(Long id) {
        return noticeMapper.selectById(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"noticeUserList", "noticeUnreadCount"}, allEntries = true)
    public Boolean updateNotice(Long id, NoticeCreateDTO dto) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) return false;

        String beforeStatus = notice.getPublishStatus();
        BeanUtils.copyProperties(dto, notice);
        notice.setTargetBuilding(dto.getBuildingNo());
        notice.setUpdateTime(LocalDateTime.now());

        if ("PUBLISHED".equalsIgnoreCase(dto.getPublishStatus()) && !"PUBLISHED".equalsIgnoreCase(beforeStatus)) {
            notice.setPublishTime(LocalDateTime.now());
        }
        
        noticeMapper.updateById(notice);
        return true;
    }

    @Override
    public List<ExpiringNoticeDTO> getExpiringNotices(Integer days) {
        if (days == null || days <= 0) days = 3;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(days);
        
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotice::getDeleted, 0)
                .eq(SysNotice::getPublishStatus, "PUBLISHED")
                .isNotNull(SysNotice::getExpireTime)
                .gt(SysNotice::getExpireTime, now)
                .le(SysNotice::getExpireTime, threshold)
                .orderByAsc(SysNotice::getExpireTime);
        
        List<SysNotice> list = noticeMapper.selectList(wrapper);
        
        return list.stream().map(n -> {
            ExpiringNoticeDTO dto = new ExpiringNoticeDTO();
            dto.setId(n.getId());
            dto.setTitle(n.getTitle());
            dto.setTargetType(n.getTargetType());
            dto.setPublishTime(n.getPublishTime());
            dto.setExpireTime(n.getExpireTime());
            dto.setTopFlag(n.getTopFlag());
            dto.setDaysLeft(java.time.Duration.between(now, n.getExpireTime()).toDays());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"noticeUserList", "noticeUnreadCount"}, allEntries = true)
    public Boolean batchExpireNotices(BatchNoticeExpireDTO dto) {
        if (dto.getNoticeIds() == null || dto.getNoticeIds().isEmpty()) {
            return false;
        }
        
        LocalDateTime expireTime = calculateExpireTime(dto.getExpireType(), dto.getCustomExpireTime(), dto.getDays());
        
        UpdateWrapper<SysNotice> wrapper = new UpdateWrapper<>();
        wrapper.in("id", dto.getNoticeIds())
                .set("expire_time", expireTime)
                .set("update_time", LocalDateTime.now());
        
        return noticeMapper.update(null, wrapper) > 0;
    }
    
    private LocalDateTime calculateExpireTime(NoticeExpireDTO.ExpireType type, LocalDateTime customTime, Integer days) {
        LocalDateTime now = LocalDateTime.now();
        switch (type) {
            case NEVER: return null;
            case CUSTOM: return customTime;
            case DAYS_7: return now.plusDays(7);
            case DAYS_30: return now.plusDays(30);
            case MONTH_3: return now.plusMonths(3);
            default: return days != null ? now.plusDays(days) : null;
        }
    }

    @Override
    @Cacheable(cacheNames = "noticeUnreadCount", key = "#userId")
    public Integer getUnreadCount(Long userId) {
        // 1. 获取用户关联的房屋信息以确定可见范围
        List<HouseDTO> userHouses = houseServiceClient.getHousesByUserId(userId);
        Set<String> communityNames = new HashSet<>();
        Set<String> buildingNos = new HashSet<>();
        if (userHouses != null) {
            for (HouseDTO h : userHouses) {
                if (h.getCommunityName() != null) communityNames.add(h.getCommunityName());
                if (h.getBuildingNo() != null) buildingNos.add(h.getBuildingNo());
            }
        }

        // 2. 查询该用户可见的所有有效通知的总数
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotice::getPublishStatus, "PUBLISHED")
                .eq(SysNotice::getDeleted, 0)
                .and(w -> {
                    w.eq(SysNotice::getTargetType, "ALL")
                     .or(sub -> sub.eq(SysNotice::getTargetType, "USER").eq(SysNotice::getTargetUserId, userId));
                    
                    if (!communityNames.isEmpty()) {
                        w.or(sub -> sub.eq(SysNotice::getTargetType, "COMMUNITY").in(SysNotice::getCommunityName, communityNames));
                    }
                    if (!buildingNos.isEmpty()) {
                        w.or(sub -> sub.eq(SysNotice::getTargetType, "BUILDING").in(SysNotice::getTargetBuilding, buildingNos));
                    }
                })
                // 排除已过期的
                .and(w -> w.isNull(SysNotice::getExpireTime).or().gt(SysNotice::getExpireTime, LocalDateTime.now()));
        
        // 注意：这里需要先查出所有可见的通知ID，然后去 sys_notice_read 表里排除已读的
        List<SysNotice> visibleNotices = noticeMapper.selectList(wrapper);
        if (visibleNotices.isEmpty()) {
            return 0;
        }
        
        List<Long> visibleIds = visibleNotices.stream().map(SysNotice::getId).collect(Collectors.toList());
        
        Long validReadCount = noticeReadMapper.selectCount(new QueryWrapper<SysNoticeRead>()
                .eq("user_id", userId)
                .eq("status", "READ")
                .in("notice_id", visibleIds));
        
        return visibleIds.size() - Math.toIntExact(validReadCount);
    }

    @Override
    public NoticeReadStatDTO getReadStat(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || notice.getDeleted() == 1) {
            return null;
        }

        // 统计该通知的已读人数
        Long readCount = noticeReadMapper.selectCount(new QueryWrapper<SysNoticeRead>()
                .eq("notice_id", noticeId)
                .eq("status", "READ"));

        // 统计目标用户总数（ALL类型无法精确统计，返回-1表示全体）
        int totalUsers = -1;
        if ("USER".equals(notice.getTargetType())) {
            totalUsers = 1;
        } else if ("BUILDING".equals(notice.getTargetType()) || "COMMUNITY".equals(notice.getTargetType())) {
            // 统计曾经收到过该通知（已读或未读记录）的用户数作为近似值
            // 实际已读人数即为 readCount，总人数用已读+未读记录估算
            Long totalRecords = noticeReadMapper.selectCount(new QueryWrapper<SysNoticeRead>()
                    .eq("notice_id", noticeId));
            totalUsers = Math.toIntExact(totalRecords);
        }

        NoticeReadStatDTO stat = new NoticeReadStatDTO();
        stat.setNoticeId(noticeId);
        stat.setTitle(notice.getTitle());
        stat.setReadCount(Math.toIntExact(readCount));
        if (totalUsers >= 0) {
            stat.setTotalUsers(totalUsers);
            stat.setUnreadCount(Math.max(0, totalUsers - Math.toIntExact(readCount)));
            stat.setReadRate(totalUsers > 0 ? (double) readCount / totalUsers : 0.0);
        } else {
            stat.setTotalUsers(-1);
            stat.setUnreadCount(-1);
            stat.setReadRate(null);
        }
        return stat;
    }
}
