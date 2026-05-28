package com.lsx.parking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.parking.dto.ParkingReserveAdminCancelDTO;
import com.lsx.parking.dto.ParkingReserveCancelDTO;
import com.lsx.parking.dto.ParkingReserveCreateDTO;
import com.lsx.parking.dto.ParkingReserveQueryDTO;
import com.lsx.parking.entity.ParkingReserve;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.mapper.ParkingReserveMapper;
import com.lsx.parking.service.ParkingReserveService;
import com.lsx.parking.service.ParkingSpaceService;
import com.lsx.parking.vo.ParkingReserveVO;
import com.lsx.parking.client.UserServiceClient;
import com.lsx.parking.dto.external.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ParkingReserveServiceImpl
        extends ServiceImpl<ParkingReserveMapper, ParkingReserve>
        implements ParkingReserveService {

    @Autowired
    private ParkingSpaceService parkingSpaceService;

    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    public Long createReserve(ParkingReserveCreateDTO dto) {
        ParkingReserve reserve = new ParkingReserve();
        reserve.setUserId(dto.getUserId());
        reserve.setSpaceId(dto.getSpaceId());
        reserve.setReserveStartTime(dto.getReserveStartTime());
        reserve.setReserveEndTime(dto.getReserveEndTime());
        reserve.setStatus("RESERVED");
        reserve.setCreateTime(LocalDateTime.now());
        reserve.setUpdateTime(LocalDateTime.now());
        this.save(reserve);
        return reserve.getId();
    }

    @Override
    public Boolean cancelReserve(ParkingReserveCancelDTO dto) {
        ParkingReserve reserve = this.getById(dto.getReserveId());
        if (reserve == null) {
            return false;
        }
        if (!reserve.getUserId().equals(dto.getUserId())) {
            return false;
        }
        reserve.setStatus("CANCELLED");
        reserve.setCancelBy("USER");
        reserve.setUpdateTime(LocalDateTime.now());
        return this.updateById(reserve);
    }

    @Override
    public Boolean adminCancelReserve(ParkingReserveAdminCancelDTO dto) {
        ParkingReserve reserve = this.getById(dto.getReserveId());
        if (reserve == null) {
            return false;
        }
        reserve.setStatus("CANCELLED");
        reserve.setCancelBy("ADMIN");
        reserve.setCancelReason(dto.getReason());
        reserve.setUpdateTime(LocalDateTime.now());
        return this.updateById(reserve);
    }

    @Override
    public IPage<ParkingReserveVO> listMyReserves(Long userId, ParkingReserveQueryDTO dto) {
        Page<ParkingReserve> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<ParkingReserve> reservePage = this.page(
                page,
                Wrappers.<ParkingReserve>lambdaQuery()
                        .eq(ParkingReserve::getUserId, userId)
                        .eq(dto.getStatus() != null && !dto.getStatus().isEmpty(), ParkingReserve::getStatus, dto.getStatus())
                        .orderByDesc(ParkingReserve::getCreateTime)
        );

        return reservePage.convert(reserve -> {
            ParkingReserveVO vo = new ParkingReserveVO();
            vo.setReserveId(reserve.getId());
            vo.setSpaceId(reserve.getSpaceId());
            vo.setStatus(reserve.getStatus());
            vo.setReserveStartTime(reserve.getReserveStartTime());
            vo.setReserveEndTime(reserve.getReserveEndTime());
            vo.setCreateTime(reserve.getCreateTime());
            vo.setStatusText(toStatusText(reserve.getStatus()));

            ParkingSpace space = parkingSpaceService.getById(reserve.getSpaceId());
            if (space != null) {
                vo.setSpaceNo(space.getSpaceNo());
                vo.setCommunityName(space.getCommunityName());
            }

            return vo;
        });
    }

    @Override
    public IPage<ParkingReserveVO> adminListReserves(ParkingReserveQueryDTO dto) {
        Page<ParkingReserve> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<ParkingReserve> reservePage = this.page(
                page,
                Wrappers.<ParkingReserve>lambdaQuery()
                        .eq(dto.getStatus() != null && !dto.getStatus().isEmpty(), ParkingReserve::getStatus, dto.getStatus())
                        .eq(dto.getUserId() != null, ParkingReserve::getUserId, dto.getUserId())
                        .orderByDesc(ParkingReserve::getCreateTime)
        );

        return reservePage.convert(reserve -> {
            ParkingReserveVO vo = new ParkingReserveVO();
            vo.setReserveId(reserve.getId());
            vo.setSpaceId(reserve.getSpaceId());
            vo.setStatus(reserve.getStatus());
            vo.setReserveStartTime(reserve.getReserveStartTime());
            vo.setReserveEndTime(reserve.getReserveEndTime());
            vo.setCreateTime(reserve.getCreateTime());
            vo.setStatusText(toStatusText(reserve.getStatus()));

            ParkingSpace space = parkingSpaceService.getById(reserve.getSpaceId());
            if (space != null) {
                vo.setSpaceNo(space.getSpaceNo());
                vo.setCommunityName(space.getCommunityName());
            }

            UserDTO user = userServiceClient.getUserById(reserve.getUserId());
            if (user != null) {
                vo.setUserId(user.getId());
                vo.setUserName(user.getRealName());
                vo.setPhone(user.getPhone());
            }

            return vo;
        });
    }

    private String toStatusText(String status) {
        if ("RESERVED".equals(status)) {
            return "预约中";
        }
        if ("EXPIRED".equals(status)) {
            return "已过期";
        }
        if ("CANCELLED".equals(status)) {
            return "已取消";
        }
        return status;
    }
}
