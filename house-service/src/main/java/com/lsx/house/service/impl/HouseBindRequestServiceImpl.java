package com.lsx.house.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.house.client.UserServiceClient;
import com.lsx.house.entity.House;
import com.lsx.house.entity.HouseBindRequest;
import com.lsx.house.entity.UserHouse;
import com.lsx.house.mapper.HouseBindRequestMapper;
import com.lsx.house.mapper.HouseMapper;
import com.lsx.house.mapper.UserHouseMapper;
import com.lsx.house.service.HouseBindAsyncService;
import com.lsx.house.service.HouseBindRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class HouseBindRequestServiceImpl extends ServiceImpl<HouseBindRequestMapper, HouseBindRequest>
        implements HouseBindRequestService {

    @Resource
    private HouseMapper houseMapper;
    @Resource
    private UserHouseMapper userHouseMapper;
    @Resource
    private UserServiceClient userServiceClient;
    @Resource
    private HouseBindAsyncService houseBindAsyncService;

    @Override
    public Page<HouseBindRequest> pageRequests(Integer pageNum, Integer pageSize, String keyword, String status, Long communityId) {
        Page<HouseBindRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<HouseBindRequest> w = new LambdaQueryWrapper<>();
        if (communityId != null) {
            w.eq(HouseBindRequest::getCommunityId, communityId);
        }
        if (StringUtils.hasText(status)) {
            w.eq(HouseBindRequest::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(HouseBindRequest::getUsername, keyword)
                    .or().like(HouseBindRequest::getRealName, keyword)
                    .or().like(HouseBindRequest::getPhone, keyword)
                    .or().like(HouseBindRequest::getCommunityName, keyword)
                    .or().like(HouseBindRequest::getBuildingNo, keyword)
                    .or().like(HouseBindRequest::getHouseNo, keyword));
        }
        w.orderByDesc(HouseBindRequest::getApplyTime);
        return this.page(page, w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long id, String identityType, Long adminId) {
        HouseBindRequest req = this.getById(id);
        if (req == null) {
            throw new RuntimeException("绑定申请不存在");
        }
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("绑定申请状态不允许审核");
        }

        House house = houseMapper.selectById(req.getHouseId());
        if (house == null) {
            throw new RuntimeException("房屋不存在");
        }
        if (house.getBindStatus() != null && house.getBindStatus() == 1) {
            throw new RuntimeException("该房屋已被其他用户绑定");
        }

        Long already = userHouseMapper.selectCount(new LambdaQueryWrapper<UserHouse>()
                .eq(UserHouse::getHouseId, req.getHouseId())
                .in(UserHouse::getStatus, "approved", "APPROVED", "1", "审核通过"));
        if (already != null && already > 0) {
            house.setBindStatus(1);
            houseMapper.updateById(house);
            throw new RuntimeException("该房屋已被其他用户绑定");
        }

        req.setIdentityType(StringUtils.hasText(identityType) ? identityType : req.getIdentityType());
        req.setStatus("APPROVED");
        req.setApproveTime(LocalDateTime.now());
        req.setApproveBy(adminId);
        req.setRejectReason(null);
        this.updateById(req);

        userHouseMapper.insertUserHouseApproved(req.getUserId(), req.getHouseId());
        house.setBindStatus(1);
        houseMapper.updateById(house);
        houseBindAsyncService.syncUserCommunityIdIfEmpty(req.getUserId(), house.getCommunityId());

        this.update(new LambdaUpdateWrapper<HouseBindRequest>()
                .set(HouseBindRequest::getStatus, "REJECTED")
                .set(HouseBindRequest::getRejectReason, "房屋已被绑定")
                .set(HouseBindRequest::getApproveTime, LocalDateTime.now())
                .set(HouseBindRequest::getApproveBy, adminId)
                .eq(HouseBindRequest::getHouseId, req.getHouseId())
                .eq(HouseBindRequest::getStatus, "PENDING")
                .ne(HouseBindRequest::getId, req.getId()));

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long id, String reason, Long adminId) {
        HouseBindRequest req = this.getById(id);
        if (req == null) {
            throw new RuntimeException("绑定申请不存在");
        }
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("绑定申请状态不允许审核");
        }
        req.setStatus("REJECTED");
        req.setRejectReason(reason);
        req.setApproveTime(LocalDateTime.now());
        req.setApproveBy(adminId);
        return this.updateById(req);
    }
}
