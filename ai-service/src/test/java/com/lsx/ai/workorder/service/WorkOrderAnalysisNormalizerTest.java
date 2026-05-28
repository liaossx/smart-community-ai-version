package com.lsx.ai.workorder.service;

import com.lsx.ai.workorder.dto.WorkOrderAnalyzeResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkOrderAnalysisNormalizerTest {

    @Test
    void fillsDerivedFieldsAndProviderMetadata() {
        WorkOrderAnalyzeResponse response = new WorkOrderAnalyzeResponse();
        response.setPriority(4);
        response.setConfidence(92);

        WorkOrderAnalyzeResponse result = new WorkOrderAnalysisNormalizer()
                .normalize(response, "spring-ai-v1", "deepseek-v4-flash");

        assertThat(result.getPriorityDesc()).isEqualTo("高危特急");
        assertThat(result.getUrgencyLevel()).isEqualTo("CRITICAL");
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
        assertThat(result.getSuggestedResponseMinutes()).isEqualTo(15);
        assertThat(result.getManualReviewNeeded()).isTrue();
        assertThat(result.getProvider()).isEqualTo("SPRING_AI");
        assertThat(result.getProviderVersion()).isEqualTo("spring-ai-v1");
        assertThat(result.getModel()).isEqualTo("deepseek-v4-flash");
        assertThat(result.getMatchedKeywords()).isEmpty();
        assertThat(result.getSafetyTips()).isEmpty();
    }

    @Test
    void clampsInvalidPriority() {
        WorkOrderAnalyzeResponse response = new WorkOrderAnalyzeResponse();
        response.setPriority(99);

        WorkOrderAnalyzeResponse result = new WorkOrderAnalysisNormalizer()
                .normalize(response, "spring-ai-v1", "deepseek-v4-flash");

        assertThat(result.getPriority()).isEqualTo(4);
        assertThat(result.getUrgencyLevel()).isEqualTo("CRITICAL");
    }

    @Test
    void raisesPriorityWhenResponseTimeIsUrgent() {
        WorkOrderAnalyzeResponse response = new WorkOrderAnalyzeResponse();
        response.setPriority(2);
        response.setUrgencyLevel("MEDIUM");
        response.setRiskLevel("LOW");
        response.setSuggestedResponseMinutes(30);

        WorkOrderAnalyzeResponse result = new WorkOrderAnalysisNormalizer()
                .normalize(response, "spring-ai-v1", "deepseek-v4-flash");

        assertThat(result.getPriority()).isEqualTo(3);
        assertThat(result.getPriorityDesc()).isEqualTo("紧急");
        assertThat(result.getUrgencyLevel()).isEqualTo("HIGH");
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(result.getSuggestedResponseMinutes()).isEqualTo(30);
    }
}
