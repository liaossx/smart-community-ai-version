package com.lsx.property.property.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.property.property.dto.CurrentFeeDTO;
import com.lsx.property.property.dto.FeeDTO;
import com.lsx.property.property.dto.FeeHistoryDTO;
import com.lsx.property.property.dto.GenerateFeeDTO;
import com.lsx.property.property.dto.PayFeeDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FeeService {

    List<CurrentFeeDTO> getCurrentUnpaid(Long userId);


    Page<FeeHistoryDTO> getPaymentHistory(Long userId, LocalDateTime startTime, LocalDateTime endTime,
                                          Integer pageNum, Integer pageSize);

    Boolean generateBills(GenerateFeeDTO dto, Long adminId);


    String payFee(PayFeeDTO dto, Long userId);

    void payCallback(String orderNo, String tradeNo, String status, BigDecimal payAmount, String payChannel);

    Page<FeeDTO> adminList(String status, String ownerName, Integer pageNum, Integer pageSize);
    boolean remind(List<Long> feeIds);
}
