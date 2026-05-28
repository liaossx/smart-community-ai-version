package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsReportRequest;
import com.lsx.ai.operations.dto.OperationsReportResponse;
import com.lsx.ai.operations.dto.OperationsRiskAlert;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperationsReportNormalizerTest {

    @Test
    void fillsMetadataAndMarksManualReviewForHighRisk() {
        OperationsReportRequest request = new OperationsReportRequest();
        request.setCommunityName("Sunny Community");
        request.setStartDate("2026-05-18");
        request.setEndDate("2026-05-24");
        request.setUrgentRepairCount(1);
        request.setResidentAppeals(List.of("parking", "noise"));

        OperationsRiskAlert risk = new OperationsRiskAlert();
        risk.setRiskLevel("danger");

        OperationsReportResponse response = new OperationsReportResponse();
        response.setRiskAlerts(List.of(risk));
        response.setConfidence(-10);

        OperationsReportResponse result = new OperationsReportNormalizer()
                .normalize(response, request, "SPRING_AI_OPERATIONS", "operations-ai-v1", "deepseek-v4-flash");

        assertThat(result.getReportTitle()).contains("Sunny Community");
        assertThat(result.getResidentAppealSummary()).contains("parking", "noise");
        assertThat(result.getRiskAlerts().get(0).getRiskLevel()).isEqualTo("LOW");
        assertThat(result.getManualReviewNeeded()).isTrue();
        assertThat(result.getConfidence()).isEqualTo(90);
        assertThat(result.getProvider()).isEqualTo("SPRING_AI_OPERATIONS");
        assertThat(result.getProviderVersion()).isEqualTo("operations-ai-v1");
        assertThat(result.getModel()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    void raisesLowModelConfidenceWhenSqlMetricsAreAvailable() {
        OperationsReportRequest request = new OperationsReportRequest();
        request.setStartDate("2026-05-18");
        request.setEndDate("2026-05-24");
        request.setRepairTotal(2);
        request.setComplaintTotal(1);
        request.setFeeUnpaidCount(1);

        OperationsReportResponse response = new OperationsReportResponse();
        response.setConfidence(8);

        OperationsReportResponse result = new OperationsReportNormalizer()
                .normalize(response, request, "SPRING_AI_OPERATIONS", "operations-ai-v1", "deepseek-v4-flash");

        assertThat(result.getConfidence()).isEqualTo(85);
    }

    @Test
    void keepsRuleFallbackConfidenceUnchanged() {
        OperationsReportRequest request = new OperationsReportRequest();
        request.setStartDate("2026-05-18");
        request.setEndDate("2026-05-24");
        request.setRepairTotal(2);

        OperationsReportResponse response = new OperationsReportResponse();
        response.setConfidence(65);

        OperationsReportResponse result = new OperationsReportNormalizer()
                .normalize(response, request, "RULE_OPERATIONS", "operations-ai-v1", "deepseek-v4-flash");

        assertThat(result.getConfidence()).isEqualTo(65);
    }
}
