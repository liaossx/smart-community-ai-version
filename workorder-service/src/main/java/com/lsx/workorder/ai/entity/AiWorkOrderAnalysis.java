package com.lsx.workorder.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_work_order_ai_analysis")
public class AiWorkOrderAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long repairId;
    private Long workOrderId;
    private String category;
    private Integer priority;
    private String urgencyLevel;
    private String riskLevel;
    private String recommendedTeam;
    private String suggestedAction;
    private String summary;
    private String extractedLocation;
    private Integer suggestedResponseMinutes;
    private String safetyTips;
    private String matchedKeywords;
    private Integer confidence;
    private Boolean manualReviewNeeded;
    private String provider;
    private String providerVersion;
    private Boolean latest;
    private LocalDateTime createTime;
}
