package com.lsx.workorder.repair.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.workorder.ai.entity.AiWorkOrderAnalysis;
import com.lsx.workorder.ai.service.AiWorkOrderAnalysisService;
import com.lsx.workorder.client.HouseServiceClient;
import com.lsx.workorder.client.UserServiceClient;
import com.lsx.workorder.dto.external.HouseDTO;
import com.lsx.workorder.dto.external.UserInfoDTO;
import com.lsx.workorder.repair.entity.Repair;
import com.lsx.workorder.repair.entity.WorkOrder;
import com.lsx.workorder.repair.mapper.WorkOrderMapper;
import com.lsx.workorder.repair.dto.RepairInfoDTO;
import com.lsx.workorder.repair.service.RepairService;
import com.lsx.workorder.repair.service.WorkOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 工单服务实现类
 */
@Slf4j
@Service
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements WorkOrderService {

    @Resource
    @Lazy
    private RepairService repairService;

    @Resource
    private HouseServiceClient houseServiceClient;

    @Resource
    private UserServiceClient userServiceClient;

    @Resource
    private AiWorkOrderAnalysisService aiWorkOrderAnalysisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder createFromRepair(Long repairId) {
        Repair repair = repairService.getById(repairId);
        if (repair == null) {
            throw new RuntimeException("报修单不存在");
        }
        
        // 检查是否已经生成过工单
        WorkOrder existingOrder = this.getOne(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getRepairId, repairId));
        if (existingOrder != null) {
            return existingOrder;
        }

        WorkOrder order = new WorkOrder();
        order.setRepairId(repairId);
        order.setOrderNo("WO" + IdUtil.getSnowflakeNextIdStr());
        order.setCommunityId(repair.getCommunityId());
        order.setStatus("PENDING");
        order.setPriority(resolveAiPriority(repairId));
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        this.save(order);
        return order;
    }

    private Integer resolveAiPriority(Long repairId) {
        try {
            AiWorkOrderAnalysis analysis;
            try {
                analysis = aiWorkOrderAnalysisService.getLatestAnalysis(repairId);
            } catch (RuntimeException e) {
                analysis = aiWorkOrderAnalysisService.analyzeRepair(repairId);
            }
            if (analysis != null && analysis.getPriority() != null) {
                return analysis.getPriority();
            }
        } catch (Exception e) {
            log.warn("AI work order priority analyze failed, use default priority. repairId={}",
                    repairId, e);
        }
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignToWorker(Long orderId, Long workerId, String workerName, String workerPhone, Integer priority) {
        if (orderId == null || workerId == null) {
            throw new RuntimeException("参数错误");
        }
        if (priority != null && (priority < 1 || priority > 4)) {
            throw new RuntimeException("参数错误");
        }
        WorkOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("参数错误");
        }
        validateAssignPermission(order, workerId);
        
        order.setWorkerId(workerId);
        order.setWorkerName(workerName);
        order.setWorkerPhone(workerPhone);
        order.setStatus("ASSIGNED");
        if (priority != null) {
            order.setPriority(priority);
        }
        order.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(order);
    }

    private void validateAssignPermission(WorkOrder order, Long workerId) {
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId == null || order.getCommunityId() == null
                    || !currentCommunityId.equals(order.getCommunityId())) {
                throw new RuntimeException("无权指派其他社区工单");
            }
        }

        UserInfoDTO worker = userServiceClient.getUserById(workerId);
        if (worker == null) {
            throw new RuntimeException("维修人员不存在");
        }
        if (!"worker".equalsIgnoreCase(worker.getRole())) {
            throw new RuntimeException("只能指派维修人员");
        }
        if (order.getCommunityId() == null || worker.getCommunityId() == null
                || !order.getCommunityId().equals(worker.getCommunityId())) {
            throw new RuntimeException("维修人员与工单不属于同一社区");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startProcess(Long orderId) {
        WorkOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("工单不存在");
        }
        
        order.setStatus("PROCESSING");
        order.setActualStartTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeWorkOrder(Long orderId, String result, String imgs) {
        WorkOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("工单不存在");
        }
        
        order.setStatus("COMPLETED");
        order.setProcessResult(result);
        order.setProcessImgs(imgs);
        order.setActualEndTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        boolean success = this.updateById(order);
        if (success) {
            // 同步更新报修单状态
            repairService.updateRepairStatus(order.getRepairId(), "completed", "工单已完成: " + result);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelWorkOrder(Long orderId) {
        WorkOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("工单不存在");
        }
        
        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        
        boolean success = this.updateById(order);
        if (success) {
             repairService.updateRepairStatus(order.getRepairId(), "cancelled", "工单已取消");
        }
        return success;
    }

    @Override
    public IPage<WorkOrder> getWorkOrders(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<WorkOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        Long currentUserId = UserContext.getCurrentUserId();

        // 权限过滤
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId == null) {
                wrapper.eq(WorkOrder::getId, -1L);
            } else {
                wrapper.eq(WorkOrder::getCommunityId, currentCommunityId);
            }
            
            // 如果是维修员角色，只看自己的工单 (假设 role 名为 worker)
            if ("worker".equalsIgnoreCase(role)) {
                wrapper.eq(WorkOrder::getWorkerId, currentUserId);
            }
        }
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(WorkOrder::getStatus, status);
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(WorkOrder::getOrderNo, keyword)
                    .or()
                    .like(WorkOrder::getWorkerName, keyword));
        }
        
        wrapper.orderByDesc(WorkOrder::getCreateTime);
        
        IPage<WorkOrder> resultPage = this.page(page, wrapper);
        List<WorkOrder> records = resultPage.getRecords();
        if (records == null || records.isEmpty()) {
            return resultPage;
        }

        List<Long> repairIds = records.stream()
                .map(WorkOrder::getRepairId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (repairIds.isEmpty()) {
            return resultPage;
        }

        Map<Long, Repair> repairMap = repairService.listByIds(repairIds).stream()
                .collect(Collectors.toMap(Repair::getId, r -> r, (a, b) -> a));

        List<Long> houseIds = repairMap.values().stream()
                .map(Repair::getHouseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, HouseDTO> houseMap = new HashMap<>();
        if (!houseIds.isEmpty()) {
            List<HouseDTO> houses = houseServiceClient.getHouseListByIds(houseIds);
            if (houses != null && !houses.isEmpty()) {
                houseMap = houses.stream().collect(Collectors.toMap(HouseDTO::getId, h -> h, (a, b) -> a));
            }
        }

        List<Long> userIds = repairMap.values().stream()
                .map(Repair::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserInfoDTO> userMap = new HashMap<>();
        for (Long uid : userIds) {
            UserInfoDTO user = userServiceClient.getUserById(uid);
            if (user != null) {
                userMap.put(uid, user);
            }
        }

        for (WorkOrder wo : records) {
            Repair repair = repairMap.get(wo.getRepairId());
            if (repair != null) {
                HouseDTO house = houseMap.get(repair.getHouseId());
                String address = null;
                if (house != null) {
                    StringBuilder sb = new StringBuilder();
                    if (StringUtils.hasText(house.getCommunityName())) {
                        sb.append(house.getCommunityName());
                    }
                    if (StringUtils.hasText(house.getBuildingNo())) {
                        if (sb.length() > 0) sb.append("-");
                        sb.append(house.getBuildingNo()).append("栋");
                    }
                    if (StringUtils.hasText(house.getHouseNo())) {
                        if (sb.length() > 0) sb.append("-");
                        sb.append(house.getHouseNo());
                    }
                    address = sb.length() == 0 ? null : sb.toString();
                }

                RepairInfoDTO info = new RepairInfoDTO();
                info.setRepairId(repair.getId());
                info.setFaultDesc(repair.getFaultDesc());
                info.setFaultType(repair.getFaultType());
                info.setAddress(address);
                UserInfoDTO owner = userMap.get(repair.getUserId());
                if (owner != null) {
                    info.setOwnerPhone(owner.getPhone());
                    info.setOwnerName(owner.getName());
                }
                if (house != null) {
                    info.setBuildingNo(house.getBuildingNo());
                    info.setHouseNo(house.getHouseNo());
                }
                wo.setRepairInfo(info);
            } else {
                wo.setRepairInfo(null);
            }
        }

        return resultPage;
    }
}
