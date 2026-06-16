package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsActionItem;
import com.lsx.ai.operations.dto.OperationsInsightCard;
import com.lsx.ai.operations.dto.OperationsInsightsResponse;
import com.lsx.ai.operations.dto.OperationsReportRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperationsInsightsNormalizerTest {

    @Test
    void marksHighRiskAndManualReviewWhenUrgentRepairsExist() {
        OperationsReportRequest request = new OperationsReportRequest();
        request.setCommunityName("Sunny Community");
        request.setStartDate("2026-05-01");
        request.setEndDate("2026-05-07");
        request.setRepairTotal(12);
        request.setUrgentRepairCount(1);

        OperationsInsightsResponse response = new OperationsInsightsResponse();
        response.setConfidence(20);

        OperationsInsightsResponse result = new OperationsInsightsNormalizer()
                .normalize(response, request, "SPRING_AI_OPERATIONS_INSIGHTS",
                        "operations-ai-v1", "deepseek-v4-flash");

        assertThat(result.getOverallRiskLevel()).isEqualTo("HIGH");
        assertThat(result.getHeadline()).contains("Sunny Community");
        assertThat(result.getManualReviewNeeded()).isTrue();
        assertThat(result.getConfidence()).isEqualTo(90);
        assertThat(result.getProvider()).isEqualTo("SPRING_AI_OPERATIONS_INSIGHTS");
    }

    @Test
    void normalizesCardsAndActionsForDashboardDisplay() {
        OperationsReportRequest request = new OperationsReportRequest();
        request.setStartDate("2026-05-01");
        request.setEndDate("2026-05-07");
        request.setComplaintPending(6);

        OperationsInsightCard card = new OperationsInsightCard();
        card.setTitle("投诉积压");
        card.setRiskLevel("danger");

        OperationsActionItem action = new OperationsActionItem();
        action.setTask("处理投诉");
        action.setPriority("urgent");

        OperationsInsightsResponse response = new OperationsInsightsResponse();
        response.setInsightCards(List.of(card));
        response.setActionItems(List.of(action));

        OperationsInsightsResponse result = new OperationsInsightsNormalizer()
                .normalize(response, request, "RULE_OPERATIONS_INSIGHTS",
                        "operations-ai-v1", "deepseek-v4-flash");

        assertThat(result.getInsightCards()).hasSize(1);
        assertThat(result.getInsightCards().get(0).getRiskLevel()).isEqualTo("LOW");
        assertThat(result.getInsightCards().get(0).getEvidence()).isEmpty();
        assertThat(result.getActionItems()).hasSize(1);
        assertThat(result.getActionItems().get(0).getPriority()).isEqualTo("P2");
        assertThat(result.getActionItems().get(0).getOwnerRole()).isEqualTo("物业运营负责人");
    }

    @Test
    void keepsRuleFallbackConfidenceUnchanged() {
        OperationsReportRequest request = new OperationsReportRequest();
        request.setStartDate("2026-05-01");
        request.setEndDate("2026-05-07");
        request.setRepairTotal(1);

        OperationsInsightsResponse response = new OperationsInsightsResponse();
        response.setConfidence(72);

        OperationsInsightsResponse result = new OperationsInsightsNormalizer()
                .normalize(response, request, "RULE_OPERATIONS_INSIGHTS",
                        "operations-ai-v1", "deepseek-v4-flash");

        assertThat(result.getConfidence()).isEqualTo(72);
    }
}
