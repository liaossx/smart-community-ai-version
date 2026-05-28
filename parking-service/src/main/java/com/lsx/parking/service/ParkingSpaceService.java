package com.lsx.parking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.parking.dto.ParkingAuthorizeDTO;
import com.lsx.parking.dto.ParkingSpaceBindDTO;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.vo.ParkingAuthorizeVO;
import com.lsx.parking.vo.ParkingSpaceRemainVO;
import com.lsx.parking.vo.ParkingSpaceVO;
import com.lsx.parking.dto.ParkingSpaceQueryDTO;

import java.util.List;

import com.lsx.parking.dto.SpaceRenewDTO;

import com.lsx.parking.dto.SpaceOpenDTO;

public interface ParkingSpaceService extends IService<ParkingSpace> {

    void openSpace(SpaceOpenDTO dto);

    void renewSpace(SpaceRenewDTO dto);

    ParkingSpaceRemainVO getRemaining(Long communityId);

    Boolean bindSpace(ParkingSpaceBindDTO dto);

    List<ParkingSpaceVO> listMySpaces(Long userId);

    Boolean authorizeSpace(Long spaceId, ParkingAuthorizeDTO dto);

    IPage<ParkingAuthorizeVO> listMyAuthorizes(Long userId, Integer pageNum, Integer pageSize);

    IPage<ParkingSpaceVO> adminListSpaces(ParkingSpaceQueryDTO dto);

    IPage<ParkingSpaceVO> listAvailableFixedSpaces(Long communityId, Integer pageNum, Integer pageSize);
}
