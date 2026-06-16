package com.lsx.ai.workorder.service;

import com.lsx.ai.workorder.dto.WorkOrderAnalyzeResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void softensAmbiguousCaseWithoutExplicitHighRiskSignal() {
        WorkOrderAnalyzeResponse response = new WorkOrderAnalyzeResponse();
        response.setPriority(4);
        response.setSummary("卫生间门口地面有点潮，不确定是不是漏水，需进一步确认。");
        response.setSuggestedAction("建议先上门看一下现场，再判断是否需要安排维修。");
        response.setMatchedKeywords(List.of("不确定", "看一下"));

        WorkOrderAnalyzeResponse result = new WorkOrderAnalysisNormalizer()
                .normalize(response, "spring-ai-v1", "deepseek-v4-flash");

        assertThat(result.getPriority()).isEqualTo(3);
        assertThat(result.getPriorityDesc()).isEqualTo("紧急");
        assertThat(result.getUrgencyLevel()).isEqualTo("HIGH");
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(result.getManualReviewNeeded()).isTrue();
    }

    @Test
    void marksAmbiguousLowPriorityCaseForManualReview() {
        WorkOrderAnalyzeResponse response = new WorkOrderAnalyzeResponse();
        response.setPriority(1);
        response.setConfidence(80);
        response.setSummary("卫生间门口地面有点潮，不确定是不是漏水，需上门检查。");
        response.setSuggestedAction("上门看一下现场并确认是否存在渗漏。");
        response.setMatchedKeywords(List.of("不确定", "看一下"));

        WorkOrderAnalyzeResponse result = new WorkOrderAnalysisNormalizer()
                .normalize(response, "spring-ai-v1", "deepseek-v4-flash");

        assertThat(result.getPriority()).isEqualTo(1);
        assertThat(result.getManualReviewNeeded()).isTrue();
        assertThat(result.getUrgencyLevel()).isEqualTo("LOW");
    }
}
