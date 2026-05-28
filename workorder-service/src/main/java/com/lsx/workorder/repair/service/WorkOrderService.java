package com.lsx.workorder.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.workorder.repair.entity.WorkOrder;

/**
 * 工单服务接口
 */
public interface WorkOrderService extends IService<WorkOrder> {
    /**
     * 为报修单生成工单
     */
    WorkOrder createFromRepair(Long repairId);
    
    /**
     * 分配工单给维修员
     */
    boolean assignToWorker(Long orderId, Long workerId, String workerName, String workerPhone, Integer priority);
    
    /**
     * 开始处理工单
     */
    boolean startProcess(Long orderId);
    
    /**
     * 完成工单
     */
    boolean completeWorkOrder(Long orderId, String result, String imgs);
    
    /**
     * 取消工单
     */
    boolean cancelWorkOrder(Long orderId);
    
    /**
     * 分页查询工单
     */
    IPage<WorkOrder> getWorkOrders(Integer pageNum, Integer pageSize, String status, String keyword);
}
