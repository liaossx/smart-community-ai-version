package com.lsx.property.property.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.client.HouseServiceClient;
import com.lsx.property.dto.external.HouseDTO;
import com.lsx.property.notice.dto.NoticeCreateDTO;
import com.lsx.property.notice.service.NoticeService;
import com.lsx.property.property.dto.*;
import com.lsx.property.property.entity.SysFee;
import com.lsx.property.property.entity.SysFeeRecord;
import com.lsx.property.property.mapper.SysFeeMapper;
import com.lsx.property.property.mapper.SysFeeRecordMapper;
import com.lsx.property.property.service.FeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FeeServiceImpl implements FeeService {

    @Resource
    private SysFeeMapper feeMapper;
    @Resource
    private SysFeeRecordMapper feeRecordMapper;
    @Resource
    private HouseServiceClient houseServiceClient;
    @Resource
    private NoticeService noticeService;


    @Override
    @Transactional
    public boolean remind(List<Long> feeIds) {
        if (feeIds == null || feeIds.isEmpty()) {
            return false;
        }

        // 1. 查询未缴纳的费用
        List<SysFee> fees = feeMapper.selectBatchIds(feeIds);
        if (fees.isEmpty()) {
            return false;
        }

        int count = 0;
        for (SysFee fee : fees) {
            if (!"UNPAID".equals(fee.getStatus())) {
                continue;
            }

            // 2. 查找业主 (通过远程调用 HouseService)
            List<Long> ownerIds = houseServiceClient.getBoundUsersByHouseId(fee.getHouseId());
            
            if (ownerIds == null || ownerIds.isEmpty()) {
                continue;
            }

            // 向所有绑定的业主发送通知
            boolean sent = false;
            for (Long userId : ownerIds) {
                NoticeCreateDTO noticeDTO = new NoticeCreateDTO();
                noticeDTO.setTitle("缴费提醒");
                noticeDTO.setContent("尊敬的业主，您有一笔物业费（" + fee.getFeeCycle() + "）尚未缴纳，请及时处理。");
                noticeDTO.setTargetType("USER");
                noticeDTO.setTargetUserId(userId);
                noticeDTO.setTopFlag(true);
                
                if (fee.getCommunityId() != null) {
                    noticeDTO.setCommunityId(fee.getCommunityId());
                    String communityName = houseServiceClient.getCommunityNameById(fee.getCommunityId());
                    if (communityName != null) noticeDTO.setCommunityName(communityName);
                }
                noticeDTO.setPublishStatus("PUBLISHED");
                
                try {
                    Long currentUserId = UserContext.getCurrentUserId();
                    if (currentUserId == null) currentUserId = 1L; // 默认系统管理员
                    noticeService.createNotice(noticeDTO, currentUserId);
                    sent = true;
                } catch (Exception e) {
                    log.error("Failed to send reminder to user {}", userId, e);
                }
            }

            // 3. 更新提醒次数
            if (sent) {
                if (fee.getRemindCount() == null) {
                    fee.setRemindCount(0);
                }
                fee.setRemindCount(fee.getRemindCount() + 1);
                feeMapper.updateById(fee);
                count++;
            }
        }

        return count > 0;
    }


    @Override
    public List<CurrentFeeDTO> getCurrentUnpaid(Long userId) {
        // 1. 查询用户绑定的房屋ID (通过 HouseServiceClient)
        List<HouseDTO> userHouses = houseServiceClient.getHousesByUserId(userId);

        if (userHouses == null || userHouses.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> houseIds = userHouses.stream()
                .map(HouseDTO::getId)
                .collect(Collectors.toList());

        // 2. 查询未缴纳账单
        QueryWrapper<SysFee> feeQuery = new QueryWrapper<>();
        feeQuery.in("house_id", houseIds).in("status", Arrays.asList("UNPAID", "PAYING")).orderByDesc("due_date");
        List<SysFee> fees = feeMapper.selectList(feeQuery);
        
        Map<Long, HouseDTO> houseMap = userHouses.stream()
                .collect(Collectors.toMap(HouseDTO::getId, h -> h));

        // 3. 关联房屋信息
        return fees.stream().map(fee -> {
            CurrentFeeDTO dto = new CurrentFeeDTO();
            BeanUtils.copyProperties(fee, dto);
            dto.setFeeId(fee.getId());
            HouseDTO houseResult = houseMap.get(fee.getHouseId());
            if (houseResult != null) {
                dto.setCommunityName(houseResult.getCommunityName());
                dto.setBuildingNo(houseResult.getBuildingNo());
                dto.setHouseNo(houseResult.getHouseNo());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<FeeHistoryDTO> getPaymentHistory(Long userId,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 Integer pageNum,
                                                 Integer pageSize) {

        Page<SysFeeRecord> page = new Page<>(pageNum, pageSize);

        QueryWrapper<SysFeeRecord> query = new QueryWrapper<>();
        query.eq("user_id", userId)
                .eq("status", "SUCCESS");

        if (startTime != null) query.ge("pay_time", startTime);
        if (endTime != null) query.le("pay_time", endTime);

        query.orderByDesc("pay_time");

        Page<SysFeeRecord> recordPage = feeRecordMapper.selectPage(page, query);

        List<Long> feeIds = recordPage.getRecords()
                .stream()
                .map(SysFeeRecord::getFeeId)
                .collect(Collectors.toList());

        Map<Long, SysFee> feeMap = new HashMap<>();

        if (!feeIds.isEmpty()) {
            List<SysFee> feeList = feeMapper.selectBatchIds(feeIds);
            feeMap.putAll(
                    feeList.stream().collect(
                            Collectors.toMap(SysFee::getId, f -> f)
                    )
            );
        }

        List<FeeHistoryDTO> dtoList = recordPage.getRecords().stream().map(record -> {
            FeeHistoryDTO dto = new FeeHistoryDTO();
            dto.setRecordId(record.getId());
            dto.setPayAmount(record.getPayAmount());
            dto.setPayType(record.getPayType());
            dto.setPayTime(record.getPayTime());
            dto.setStatus(record.getStatus());

            SysFee fee = feeMap.get(record.getFeeId());
            if (fee != null) {
                dto.setFeeCycle(fee.getFeeCycle());
                dto.setFeeType(fee.getFeeType());
            }

            return dto;
        }).collect(Collectors.toList());

        Page<FeeHistoryDTO> result = new Page<>(pageNum, pageSize);
        result.setTotal(recordPage.getTotal());
        result.setRecords(dtoList);

        return result;
    }


    @Override
    @Transactional
    public Boolean generateBills(GenerateFeeDTO dto, Long adminId) {
        // 1. 权限与数据隔离校验
        Long currentUserId = UserContext.getCurrentUserId();
        String currentUserRole = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();

        log.info("管理员[{}]尝试生成账单，角色: {}, 社区ID: {}, 参数: {}", currentUserId, currentUserRole, communityId, dto);

        if (!"super_admin".equalsIgnoreCase(currentUserRole)) {
            if (communityId == null) {
                log.error("普通管理员未绑定社区，无法生成账单");
                return false;
            }
            String communityName = houseServiceClient.getCommunityNameById(communityId);
            if (communityName == null) {
                log.error("未找到ID为{}的社区信息", communityId);
                return false;
            }
            log.info("普通管理员权限限制：将社区名从[{}]修正为归属社区[{}]", dto.getCommunityName(), communityName);
            dto.setCommunityName(communityName);
        }

        LocalDateTime dueDateTime = parseDueDate(dto.getDueDate());

        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("单价必须大于0");
        }
        if (dto.getFeeCycle() == null || dto.getFeeCycle().trim().isEmpty()) {
            throw new RuntimeException("费用周期不能为空");
        }

        // 1. 查询已绑定且审核通过的房屋 (通过 HouseServiceClient)
        List<Long> boundHouseIds = houseServiceClient.getBoundHouseIds(dto.getCommunityName(), dto.getBuildingNo());
        
        if (boundHouseIds == null || boundHouseIds.isEmpty()) {
            log.warn("生成账单失败：未找到已绑定用户的房屋");
            return false;
        }

        // 2. 查询房屋详细信息
        List<HouseDTO> targetHouses = houseServiceClient.getHouseListByIds(boundHouseIds);
        if (targetHouses == null || targetHouses.isEmpty()) {
            log.warn("生成账单失败：未找到房屋详细信息");
            return false;
        }

        // 3. 检查是否已存在相同周期的账单
        checkDuplicateBills(boundHouseIds, dto.getFeeCycle());

        // 4. 生成账单
        int successCount = 0;
        for (HouseDTO house : targetHouses) {
            try {
                // 使用 HouseDTO 中的 area 字段
                if (house.getArea() == null || house.getArea() <= 0) {
                    log.warn("房屋{}面积数据异常，跳过账单生成", house.getId());
                    continue;
                }
                
                BigDecimal area = BigDecimal.valueOf(house.getArea());
                
                SysFee fee = new SysFee();
                fee.setHouseId(house.getId());
                fee.setBuildingNo(house.getBuildingNo());
                if (house.getCommunityId() != null) {
                    fee.setCommunityId(house.getCommunityId());
                }

                fee.setFeeCycle(dto.getFeeCycle());
                fee.setFeeAmount(area.multiply(dto.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
                fee.setFeeType("物业费");
                fee.setStatus("UNPAID");
                fee.setDueDate(dueDateTime);
                fee.setCreateTime(LocalDateTime.now());
                fee.setUpdateTime(LocalDateTime.now());
                fee.setRemark("自动生成：" + dto.getFeeCycle() + "物业费");

                feeMapper.insert(fee);
                successCount++;

            } catch (Exception e) {
                log.error("生成房屋{}的账单失败：{}", house.getId(), e.getMessage());
            }
        }

        log.info("管理员[{}]生成账单成功，共{}户，成功{}户", adminId, targetHouses.size(), successCount);
        return successCount > 0;
    }
    

    /**
     * 校验重复账单
     */
    private void checkDuplicateBills(List<Long> houseIds, String feeCycle) {
        if (houseIds == null || houseIds.isEmpty()) return;
        
        QueryWrapper<SysFee> query = new QueryWrapper<>();
        query.in("house_id", houseIds)
                .eq("fee_cycle", feeCycle)
                .eq("fee_type", "物业费")
                .in("status", Arrays.asList("UNPAID", "PAID"));
        long duplicateCount = feeMapper.selectCount(query);
        if (duplicateCount > 0) {
            throw new RuntimeException("部分房屋已存在 " + feeCycle + " 物业费账单");
        }
    }


    @Override
    @Transactional
    public String payFee(PayFeeDTO dto, Long userId) {
        // 校验账单
        SysFee fee = feeMapper.selectById(dto.getFeeId());
        if (fee == null || !"UNPAID".equals(fee.getStatus())) {
            throw new RuntimeException("账单不存在或已缴费");
        }

        // 校验房屋绑定权限 (远程调用)
        List<HouseDTO> userHouses = houseServiceClient.getHousesByUserId(userId);
        boolean isBound = userHouses.stream().anyMatch(h -> h.getId().equals(fee.getHouseId()));
        
        if (!isBound) {
            throw new RuntimeException("未绑定该房屋，无法缴费");
        }

        // 生成支付记录
        String orderNo = "FEE_" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        SysFeeRecord record = new SysFeeRecord();
        BeanUtils.copyProperties(fee, record);
        record.setFeeId(fee.getId());
        record.setUserId(userId);
        record.setPayType(dto.getPayType());
        record.setPayAmount(fee.getFeeAmount());
        record.setOrderNo(orderNo);
        record.setStatus("PENDING");
        feeRecordMapper.insert(record);

        // 更新账单状态
        fee.setStatus("PAYING");
        fee.setUpdateTime(LocalDateTime.now());
        feeMapper.updateById(fee);

        return orderNo;
    }


    @Override
    @Transactional
    public void payCallback(String orderNo, String tradeNo, String status, BigDecimal payAmount, String payChannel) {
        QueryWrapper<SysFeeRecord> query = new QueryWrapper<>();
        query.eq("order_no", orderNo);
        SysFeeRecord record = feeRecordMapper.selectOne(query);
        if (record == null) {
            log.error("支付回调失败：未找到订单号{}的记录", orderNo);
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(record.getStatus())) {
            if (record.getTradeNo() != null && tradeNo != null && !record.getTradeNo().equals(tradeNo)) {
                throw new RuntimeException("重复回调流水号不一致");
            }
            return;
        }

        if (payAmount != null && record.getPayAmount() != null) {
            BigDecimal expected = record.getPayAmount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal actual = payAmount.setScale(2, RoundingMode.HALF_UP);
            if (expected.compareTo(actual) != 0) {
                throw new RuntimeException("支付金额不一致");
            }
        }

        String finalStatus = "SUCCESS".equalsIgnoreCase(status) ? "SUCCESS" : "FAIL";

        UpdateWrapper<SysFeeRecord> recordUpdate = new UpdateWrapper<>();
        recordUpdate.eq("order_no", orderNo)
                .ne("status", "SUCCESS")
                .set("trade_no", tradeNo)
                .set("pay_time", LocalDateTime.now())
                .set("status", finalStatus)
                .set("remark", payChannel);
        int updated = feeRecordMapper.update(null, recordUpdate);
        if (updated == 0) {
            SysFeeRecord latest = feeRecordMapper.selectOne(new QueryWrapper<SysFeeRecord>().eq("order_no", orderNo));
            if (latest != null && "SUCCESS".equalsIgnoreCase(latest.getStatus())) {
                return;
            }
            throw new RuntimeException("支付回调处理失败");
        }

        if ("SUCCESS".equals(finalStatus)) {
            UpdateWrapper<SysFee> feeUpdate = new UpdateWrapper<>();
            feeUpdate.eq("id", record.getFeeId())
                    .ne("status", "PAID")
                    .set("status", "PAID")
                    .set("update_time", LocalDateTime.now());
            feeMapper.update(null, feeUpdate);
        }
    }

    @Override
    public Page<FeeDTO> adminList(String status, String ownerName, Integer pageNum, Integer pageSize) {
        Page<FeeDTO> page = new Page<>(pageNum, pageSize);
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        Long filterCid = null;
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (communityId != null) filterCid = communityId;
            else filterCid = -1L;
        }
        
        return (Page<FeeDTO>) feeMapper.selectAdminList(page, status, ownerName, filterCid);
    }


    private LocalDateTime parseDueDate(String dueDateStr) {
        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            throw new RuntimeException("截止日期不能为空");
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dueDateStr, formatter);
        } catch (DateTimeParseException e1) {
            try {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate date = LocalDate.parse(dueDateStr, dateFormatter);
                return date.atTime(23, 59, 59);
            } catch (DateTimeParseException e2) {
                throw new RuntimeException("日期格式错误，支持 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd 格式");
            }
        }
    }
}
