package com.lsx.parking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.parking.dto.ParkingReserveAdminCancelDTO;
import com.lsx.parking.dto.ParkingReserveCancelDTO;
import com.lsx.parking.dto.ParkingReserveCreateDTO;
import com.lsx.parking.dto.ParkingReserveQueryDTO;
import com.lsx.parking.entity.ParkingReserve;
import com.lsx.parking.vo.ParkingReserveVO;

public interface ParkingReserveService extends IService<ParkingReserve> {

    Long createReserve(ParkingReserveCreateDTO dto);

    Boolean cancelReserve(ParkingReserveCancelDTO dto);

    Boolean adminCancelReserve(ParkingReserveAdminCancelDTO dto);

    IPage<ParkingReserveVO> listMyReserves(Long userId, ParkingReserveQueryDTO dto);

    IPage<ParkingReserveVO> adminListReserves(ParkingReserveQueryDTO dto);
}


