package com.lsx.workorder.ai.dto;

import lombok.Data;

@Data
public class AiServiceWorkOrderAnalyzeRequest {
    private Long repairId;
    private String faultType;
    private String faultDesc;
}
