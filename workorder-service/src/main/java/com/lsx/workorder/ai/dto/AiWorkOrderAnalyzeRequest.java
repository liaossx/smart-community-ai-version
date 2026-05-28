package com.lsx.workorder.ai.dto;

import lombok.Data;

@Data
public class AiWorkOrderAnalyzeRequest {
    private String faultType;
    private String faultDesc;
    private String address;
}
