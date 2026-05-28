package com.lsx.workorder.stats.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.core.common.Util.UserContext;
import com.lsx.workorder.repair.entity.Repair;
import com.lsx.workorder.repair.entity.WorkOrder;
import com.lsx.workorder.repair.service.RepairService;
import com.lsx.workorder.repair.service.WorkOrderService;
import com.lsx.workorder.repair.vo.RepairStatsResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WorkorderStatsController {

    @Resource
    private RepairService repairService;

    @Resource
    private WorkOrderService workOrderService;

    @GetMapping({"/repair/stats", "/repair/admin/stats"})
    public Map<String, Object> getRepairStats() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();

        RepairStatsResult repairResult = repairService.getRepairStats();
        Map<String, Object> repairStats = new HashMap<>();
        if (repairResult != null) {
            repairStats.put("total", repairResult.getTotal());
            repairStats.put("pending", repairResult.getPending());
            repairStats.put("processing", repairResult.getProcessing());
            repairStats.put("completed", repairResult.getCompleted());
            repairStats.put("cancelled", repairResult.getCancelled());
        } else {
            repairStats.put("total", 0);
            repairStats.put("pending", 0);
        }

        // 工单统计
        LambdaQueryWrapper<WorkOrder> orderWrapper = new LambdaQueryWrapper<>();
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            orderWrapper.eq(WorkOrder::getCommunityId, communityId);
        }
        
        Map<String, Object> workorderStats = new HashMap<>();
        workorderStats.put("total", workOrderService.count(orderWrapper));
        workorderStats.put("pending", workOrderService.count(orderWrapper.clone().eq(WorkOrder::getStatus, "PENDING")));
        workorderStats.put("assigned", workOrderService.count(orderWrapper.clone().eq(WorkOrder::getStatus, "ASSIGNED")));
        workorderStats.put("processing", workOrderService.count(orderWrapper.clone().eq(WorkOrder::getStatus, "PROCESSING")));
        workorderStats.put("completed", workOrderService.count(orderWrapper.clone().eq(WorkOrder::getStatus, "COMPLETED")));
        
        Map<String, Object> result = new HashMap<>();
        result.put("repair", repairStats);
        result.put("workorder", workorderStats);

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LambdaQueryWrapper<Repair> repairTodayWrapper = new LambdaQueryWrapper<>();
        repairTodayWrapper.ge(Repair::getCreateTime, todayStart).le(Repair::getCreateTime, todayEnd);
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            repairTodayWrapper.eq(Repair::getCommunityId, communityId);
        }
        long todayCount = repairService.count(repairTodayWrapper);
        result.put("todayRepair", todayCount);

        return result;
    }

    @GetMapping({"/repair/stats/trend", "/repair/admin/stats/trend"})
    public List<Map<String, Object>> getRepairTrend() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();

        List<Map<String, Object>> repairTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<Repair> trendWrapper = new LambdaQueryWrapper<>();
            trendWrapper.ge(Repair::getCreateTime, startOfDay)
                    .le(Repair::getCreateTime, endOfDay);

            if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
                trendWrapper.eq(Repair::getCommunityId, communityId);
            }

            long count = repairService.count(trendWrapper);

            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date.format(dateFmt));
            dayStat.put("count", count);
            repairTrend.add(dayStat);
        }
        return repairTrend;
    }
}
