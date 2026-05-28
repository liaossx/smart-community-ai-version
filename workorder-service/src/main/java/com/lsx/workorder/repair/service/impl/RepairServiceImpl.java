package com.lsx.workorder.repair.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.common.constant.MqConstants;
import com.lsx.core.common.dto.mq.RepairMsgDTO;
import com.lsx.workorder.client.HouseServiceClient;
import com.lsx.workorder.dto.external.HouseDTO;
import com.lsx.workorder.repair.dto.RepairDto;
import com.lsx.workorder.repair.entity.Repair;
import com.lsx.workorder.repair.mapper.RepairMapper;
import com.lsx.workorder.repair.service.RepairService;
import com.lsx.workorder.repair.service.WorkOrderService;
import com.lsx.workorder.repair.vo.RepairResult;
import com.lsx.workorder.repair.vo.RepairStatsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RepairServiceImpl extends ServiceImpl<RepairMapper, Repair> implements RepairService {

    @Autowired
    private HouseServiceClient houseServiceClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Resource
    @Lazy
    private WorkOrderService workOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitRepair(RepairDto repairdto) {
        String buildingNo = repairdto.getBuildingNo();
        String houseNo = repairdto.getHouseNo();
        if (buildingNo == null || buildingNo.trim().isEmpty()) {
            throw new RuntimeException("楼栋号不能为空");
        }
        if (houseNo == null || houseNo.trim().isEmpty()) {
            throw new RuntimeException("房屋号不能为空");
        }

        String housePattern = "^\\d{3,4}$";
        if (!ReUtil.isMatch(housePattern, houseNo.trim())) {
            throw new RuntimeException("房屋号格式不正确，应为3-4位数字，如'101'");
        }

        HouseDTO house = houseServiceClient.getHouseByInfo(buildingNo, houseNo);
        if (house == null) {
            throw new RuntimeException("房屋信息不存在或未绑定所属社区");
        }
        Long houseId = house.getId();

        Repair repair = new Repair();
        BeanUtil.copyProperties(repairdto, repair);
        repair.setHouseId(houseId);
        repair.setStatus("pending");
        if (UserContext.getCommunityId() != null) {
            repair.setCommunityId(UserContext.getCommunityId());
        }
        
        boolean success = baseMapper.insert(repair) > 0;
        
        if (success) {
            // 发送异步消息通知
            try {
                RepairMsgDTO msg = RepairMsgDTO.builder()
                        .repairId(repair.getId())
                        .userId(repair.getUserId())
                        .communityId(repair.getCommunityId())
                        .faultType(repair.getFaultType())
                        .createTime(LocalDateTime.now())
                        .build();
                rabbitTemplate.convertAndSend(MqConstants.REPAIR_EXCHANGE, MqConstants.REPAIR_SUBMIT_ROUTING_KEY, msg);
                log.info("已发送报修异步消息: {}", msg);
            } catch (Exception e) {
                log.error("发送报修异步消息失败", e);
                // 消息发送失败不应影响主流程，仅打日志
            }
        }
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRepairStatus(Long repairId, String status, String remark) {
        Repair repair = baseMapper.selectById(repairId);
        if (repair == null) {
            return false;
        }
        repair.setStatus(status);
        repair.setHandleRemark(remark);
        boolean success = baseMapper.updateById(repair) > 0;
        
        // 如果状态变更为 processing，自动生成工单
        if (success && "processing".equalsIgnoreCase(status)) {
            workOrderService.createFromRepair(repairId);
        }
        
        return success;
    }

    @Override
    public IPage<RepairResult> getMyRepairs(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Page<Repair> page = new Page<>(pageNum, pageSize);

        IPage<Repair> repairPage = baseMapper.selectPage(page, Wrappers.<Repair>lambdaQuery()
                .eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime));

        return repairPage.convert(this::convertToRepairResult);
    }

    @Override
    public IPage<RepairResult> getAllRepairs(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<Repair> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Repair> queryWrapper = new LambdaQueryWrapper<Repair>()
                .orderByDesc(Repair::getCreateTime);

        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId == null) {
                queryWrapper.eq(Repair::getId, -1L);
            } else {
                queryWrapper.eq(Repair::getCommunityId, currentCommunityId);
            }
        }

        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Repair::getStatus, status);
        }

        if (StringUtils.hasText(keyword)) {
            List<String> searchTerms = processSearchKeyword(keyword);
            final List<Long> houseIds = getAllMatchingHouseIds(searchTerms);

            queryWrapper.and(wrapper -> {
                for (String term : searchTerms) {
                    wrapper.like(Repair::getFaultType, term)
                            .or()
                            .like(Repair::getFaultDesc, term);
                }

                if (!houseIds.isEmpty()) {
                    wrapper.or().in(Repair::getHouseId, houseIds);
                }
            });
        }

        IPage<Repair> repairPage = baseMapper.selectPage(page, queryWrapper);
        return repairPage.convert(this::convertToRepairResult);
    }

    private List<String> processSearchKeyword(String keyword) {
        List<String> searchTerms = new ArrayList<>();
        searchTerms.add(keyword);

        if (keyword.contains("栋")) {
            String[] parts = keyword.split("栋");
            if (parts.length > 0) {
                String buildingPart = parts[0] + "栋";
                searchTerms.add(buildingPart);
            }

            if (parts.length > 1 && !parts[1].isEmpty()) {
                String housePart = parts[1];
                housePart = housePart.replace("室", "");
                if (!housePart.isEmpty()) {
                    searchTerms.add(housePart);
                }
            }
        }

        return searchTerms.stream().distinct().collect(Collectors.toList());
    }

    private List<Long> getAllMatchingHouseIds(List<String> searchTerms) {
        Set<Long> houseIds = new HashSet<>();

        for (String term : searchTerms) {
            houseIds.addAll(getHouseIdsByKeyword(term));
        }

        return new ArrayList<>(houseIds);
    }

    private List<Long> getHouseIdsByKeyword(String keyword) {
        return houseServiceClient.searchHouseIds(keyword);
    }

    @Override
    public IPage<RepairResult> getUserRepairs(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Page<Repair> page = new Page<>(pageNum, pageSize);

        IPage<Repair> repairPage = baseMapper.selectPage(page, Wrappers.<Repair>lambdaQuery()
                .eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime));

        return repairPage.convert(this::convertToRepairResult);
    }

    private RepairResult convertToRepairResult(Repair repair) {
        if (repair == null) return null;

        RepairResult result = new RepairResult();
        result.setId(repair.getId());
        result.setFaultType(repair.getFaultType());
        result.setFaultDesc(repair.getFaultDesc());
        result.setStatus(repair.getStatus());
        result.setHandleRemark(repair.getHandleRemark());
        result.setCreateTime(repair.getCreateTime());
        result.setUpdateTime(repair.getUpdateTime());

        if (StringUtils.hasText(repair.getFaultImgs())) {
            List<String> imgs = Arrays.asList(repair.getFaultImgs().split(","));
            result.setFaultImgs(imgs);
        } else {
            result.setFaultImgs(new ArrayList<>());
        }

        if (repair.getHouseId() != null) {
            HouseDTO house = houseServiceClient.getHouseById(repair.getHouseId());
            if (house != null) {
                result.setCommunityName(house.getCommunityName());
                result.setBuildingNo(house.getBuildingNo());
                result.setHouseNo(house.getHouseNo());
            }
        }

        result.setStatusDesc(getStatusDesc(repair.getStatus()));

        return result;
    }

    private String getStatusDesc(String status) {
        switch (status) {
            case "pending":
                return "待处理";
            case "processing":
                return "处理中";
            case "completed":
                return "已完成";
            case "cancelled":
                return "已取消";
            default:
                return status;
        }
    }

    @Override
    public boolean batchUpdateStatus(List<Long> repairIds, String status, String remark) {
        if (repairIds == null || repairIds.isEmpty()) {
            return false;
        }

        List<String> validStatuses = Arrays.asList("pending", "processing", "completed", "cancelled");
        if (!validStatuses.contains(status)) {
            throw new RuntimeException("状态不合法");
        }

        List<Repair> repairs = new ArrayList<>();
        for (Long repairId : repairIds) {
            Repair repair = new Repair();
            repair.setId(repairId);
            repair.setStatus(status);
            repair.setHandleRemark(remark);
            repairs.add(repair);
        }

        return this.updateBatchById(repairs);
    }

    @Override
    public void exportRepairs(String status, String keyword, HttpServletResponse response) {
        LambdaQueryWrapper<Repair> queryWrapper = new LambdaQueryWrapper<Repair>()
                .orderByDesc(Repair::getCreateTime);

        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId == null) {
                queryWrapper.eq(Repair::getId, -1L);
            } else {
                queryWrapper.eq(Repair::getCommunityId, currentCommunityId);
            }
        }

        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Repair::getStatus, status);
        }

        if (StringUtils.hasText(keyword)) {
            List<String> searchTerms = processSearchKeyword(keyword);
            final List<Long> houseIds = getAllMatchingHouseIds(searchTerms);

            queryWrapper.and(wrapper -> {
                for (String term : searchTerms) {
                    wrapper.like(Repair::getFaultType, term)
                            .or()
                            .like(Repair::getFaultDesc, term);
                }

                if (!houseIds.isEmpty()) {
                    wrapper.or().in(Repair::getHouseId, houseIds);
                }
            });
        }

        List<Repair> repairs = this.list(queryWrapper);

        List<RepairResult> repairResults = repairs.stream()
                .map(this::convertToRepairResult)
                .collect(Collectors.toList());

        try {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=repairs.csv");

            try (PrintWriter writer = response.getWriter()) {
                writer.println("ID,社区名称,楼栋号,房屋号,故障类型,故障描述,状态,处理备注,创建时间");

                for (RepairResult result : repairResults) {
                    writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                            result.getId(),
                            result.getCommunityName(),
                            result.getBuildingNo(),
                            result.getHouseNo(),
                            result.getFaultType(),
                            result.getFaultDesc(),
                            result.getStatusDesc(),
                            result.getHandleRemark() != null ? result.getHandleRemark() : "",
                            result.getCreateTime());
                }

                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public RepairStatsResult getRepairStats() {
        RepairStatsResult stats = new RepairStatsResult();

        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();

        LambdaQueryWrapper<Repair> baseWrapper = Wrappers.lambdaQuery(Repair.class);

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId != null) {
                baseWrapper.eq(Repair::getCommunityId, currentCommunityId);
            } else {
                baseWrapper.eq(Repair::getId, -1L);
            }
        }

        stats.setTotal(Math.toIntExact(this.count(baseWrapper)));

        LambdaQueryWrapper<Repair> pendingQuery = baseWrapper.clone().eq(Repair::getStatus, "pending");
        stats.setPending(Math.toIntExact(this.count(pendingQuery)));

        LambdaQueryWrapper<Repair> processingQuery = baseWrapper.clone().eq(Repair::getStatus, "processing");
        stats.setProcessing(Math.toIntExact(this.count(processingQuery)));

        LambdaQueryWrapper<Repair> completedQuery = baseWrapper.clone().eq(Repair::getStatus, "completed");
        stats.setCompleted(Math.toIntExact(this.count(completedQuery)));

        LambdaQueryWrapper<Repair> cancelledQuery = baseWrapper.clone().eq(Repair::getStatus, "cancelled");
        stats.setCancelled(Math.toIntExact(this.count(cancelledQuery)));

        return stats;
    }
}
