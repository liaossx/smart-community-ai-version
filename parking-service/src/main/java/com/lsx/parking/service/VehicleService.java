package com.lsx.parking.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.parking.dto.VehicleBindDTO;
import com.lsx.parking.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.parking.dto.ParkingCarAuditDTO;
import com.lsx.parking.vo.ParkingCarAuditVO;

public interface VehicleService {
    void bindVehicle(VehicleBindDTO dto);

    IPage<ParkingCarAuditVO> listAudit(String status, Integer pageNum, Integer pageSize);

    void auditCar(ParkingCarAuditDTO dto);
}
