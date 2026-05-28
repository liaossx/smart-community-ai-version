package com.lsx.workorder.repair.dto;

import lombok.Data;

/**
 * 工单完成 DTO
 */
@Data
public class WorkOrderCompleteDTO {
    private Long orderId;
    private String result;
    private String images; // 前端传的是 images，对应后端的 processImgs
}
