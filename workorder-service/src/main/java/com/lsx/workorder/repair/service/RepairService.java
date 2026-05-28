package com.lsx.workorder.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.workorder.repair.dto.RepairDto;
import com.lsx.workorder.repair.entity.Repair;
import com.lsx.workorder.repair.vo.RepairResult;
import com.lsx.workorder.repair.vo.RepairStatsResult;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface RepairService extends IService<Repair> {
    boolean submitRepair(RepairDto repairdto);

    boolean updateRepairStatus(Long repairId, String status, String remark);

    IPage<RepairResult> getMyRepairs(Long userId, Integer pageNum, Integer pageSize);

    IPage<RepairResult> getAllRepairs(Integer pageNum, Integer pageSize, String status, String keyword);

    IPage<RepairResult> getUserRepairs(Long userId, Integer pageNum, Integer pageSize);

    boolean batchUpdateStatus(List<Long> repairIds, String status, String remark);

    void exportRepairs(String status, String keyword, HttpServletResponse response);

    RepairStatsResult getRepairStats();
}
