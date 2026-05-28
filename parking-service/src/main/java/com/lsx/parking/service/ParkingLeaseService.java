package com.lsx.parking.service;

import com.lsx.parking.dto.ParkingLeaseOrderCreateDTO;
import com.lsx.parking.dto.ParkingLeaseOrderPayDTO;

public interface ParkingLeaseService {
    Long createLeaseOrder(ParkingLeaseOrderCreateDTO dto);
    void payLeaseOrder(ParkingLeaseOrderPayDTO dto); // 改成 DTO
}
