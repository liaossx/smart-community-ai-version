package com.lsx.parking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.parking.dto.ParkingOrderCreateDTO;
import com.lsx.parking.dto.ParkingOrderPayDTO;
import com.lsx.parking.entity.ParkingOrder;
import com.lsx.parking.vo.ParkingOrderVO;

public interface ParkingOrderService extends IService<ParkingOrder> {

    Long createOrder(ParkingOrderCreateDTO dto);

    IPage<ParkingOrderVO> listMyOrders(Long userId, Integer pageNum, Integer pageSize);

    Boolean payOrder(Long orderId, ParkingOrderPayDTO dto);

    /**
     * 管理员樺垎椤垫煡璇㈠仠杞﹁鍗?     */
    IPage<ParkingOrder> adminListOrders(Integer pageNum, Integer pageSize, String plateNo, String status, String orderType, String startDate, String endDate);
}



