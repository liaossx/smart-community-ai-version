package com.lsx.parking.service;

import com.lsx.parking.dto.ParkingGateEnterDTO;
import com.lsx.parking.dto.ParkingGateExitDTO;
import com.lsx.parking.dto.ParkingGateOpenDTO;
import com.lsx.parking.vo.ParkingGateExitResult;

public interface ParkingGateService {

    /**
     * 固定车位开始€闂?     */


     void enterGate(ParkingGateEnterDTO dto);

    ParkingGateExitResult exitGate(ParkingGateExitDTO dto);
}
