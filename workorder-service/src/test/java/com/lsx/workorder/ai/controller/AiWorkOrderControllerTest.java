package com.lsx.workorder.ai.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.workorder.ai.entity.AiWorkOrderAnalysis;
import com.lsx.workorder.ai.service.AiWorkOrderAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiWorkOrderControllerTest {

    @Test
    void analyzeRepairReturnsCreatedSnapshotInResultEnvelope() {
        AiWorkOrderAnalysis snapshot = new AiWorkOrderAnalysis();
        snapshot.setId(1L);
        snapshot.setRepairId(100L);
        snapshot.setLatest(true);

        AiWorkOrderAnalysisService service = mock(AiWorkOrderAnalysisService.class);
        when(service.analyzeRepair(100L)).thenReturn(snapshot);

        AiWorkOrderController controller = new AiWorkOrderController(service);

        Result<AiWorkOrderAnalysis> result = controller.analyzeRepair(100L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isSameAs(snapshot);
        assertThat(result.getData().getRepairId()).isEqualTo(100L);
        assertThat(result.getData().getLatest()).isTrue();
    }

    @Test
    void latestRepairAnalysisReturnsLatestSnapshotInResultEnvelope() {
        AiWorkOrderAnalysis snapshot = new AiWorkOrderAnalysis();
        snapshot.setId(2L);
        snapshot.setRepairId(100L);
        snapshot.setCategory("WATER");
        snapshot.setPriority(3);
        snapshot.setUrgencyLevel("HIGH");
        snapshot.setRiskLevel("MEDIUM");
        snapshot.setRecommendedTeam("\u6c34\u6696\u7ef4\u4fee\u7ec4");
        snapshot.setSuggestedAction("\u5efa\u8bae\u5148\u8054\u7cfb\u4f4f\u6237\u786e\u8ba4\u6f0f\u6c34\u8303\u56f4\u3002");
        snapshot.setSummary("\u536b\u751f\u95f4\u6c34\u7ba1\u6f0f\u6c34");
        snapshot.setExtractedLocation("3\u680b2\u5355\u51431801\u536b\u751f\u95f4");
        snapshot.setSuggestedResponseMinutes(30);
        snapshot.setSafetyTips("[\"\u5173\u95ed\u9600\u95e8\"]");
        snapshot.setMatchedKeywords("pipe,leak");
        snapshot.setConfidence(90);
        snapshot.setManualReviewNeeded(false);
        snapshot.setProvider("RULE");
        snapshot.setProviderVersion("rule-v1.1");
        snapshot.setLatest(true);

        AiWorkOrderAnalysisService service = mock(AiWorkOrderAnalysisService.class);
        when(service.getLatestAnalysis(100L)).thenReturn(snapshot);

        AiWorkOrderController controller = new AiWorkOrderController(service);

        Result<AiWorkOrderAnalysis> result = controller.getLatestAnalysis(100L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isSameAs(snapshot);
        assertThat(result.getData().getCategory()).isEqualTo("WATER");
        assertThat(result.getData().getUrgencyLevel()).isEqualTo("HIGH");
        assertThat(result.getData().getExtractedLocation()).isEqualTo("3\u680b2\u5355\u51431801\u536b\u751f\u95f4");
        assertThat(result.getData().getSuggestedResponseMinutes()).isEqualTo(30);
        assertThat(result.getData().getSafetyTips()).contains("\u5173\u95ed\u9600\u95e8");
        assertThat(result.getData().getMatchedKeywords()).isEqualTo("pipe,leak");
        assertThat(result.getData().getProvider()).isEqualTo("RULE");
        assertThat(result.getData().getProviderVersion()).isEqualTo("rule-v1.1");
    }

    @Test
    void latestRepairAnalysisReturnsFailureWhenNoSnapshotExists() {
        AiWorkOrderAnalysisService service = mock(AiWorkOrderAnalysisService.class);
        when(service.getLatestAnalysis(100L)).thenThrow(new RuntimeException("AI analysis snapshot not found"));

        AiWorkOrderController controller = new AiWorkOrderController(service);

        Result<AiWorkOrderAnalysis> result = controller.getLatestAnalysis(100L);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMsg()).isEqualTo("AI analysis snapshot not found");
        assertThat(result.getData()).isNull();
    }

    @Test
    void analysisHistoryReturnsSnapshotsInResultEnvelope() {
        AiWorkOrderAnalysis older = new AiWorkOrderAnalysis();
        older.setId(1L);
        older.setRepairId(100L);
        older.setCategory("OTHER");
        older.setLatest(false);

        AiWorkOrderAnalysis latest = new AiWorkOrderAnalysis();
        latest.setId(2L);
        latest.setRepairId(100L);
        latest.setCategory("WATER");
        latest.setLatest(true);

        AiWorkOrderAnalysisService service = mock(AiWorkOrderAnalysisService.class);
        when(service.getAnalysisHistory(100L)).thenReturn(Arrays.asList(latest, older));

        AiWorkOrderController controller = new AiWorkOrderController(service);

        Result<List<AiWorkOrderAnalysis>> result = controller.getAnalysisHistory(100L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsExactly(latest, older);
        assertThat(result.getData().get(0).getLatest()).isTrue();
        assertThat(result.getData().get(1).getLatest()).isFalse();
    }

    @Test
    void analysisHistoryReturnsFailureWhenRepairReportDoesNotExist() {
        AiWorkOrderAnalysisService service = mock(AiWorkOrderAnalysisService.class);
        when(service.getAnalysisHistory(404L)).thenThrow(new RuntimeException("Repair report not found"));

        AiWorkOrderController controller = new AiWorkOrderController(service);

        Result<List<AiWorkOrderAnalysis>> result = controller.getAnalysisHistory(404L);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMsg()).isEqualTo("Repair report not found");
        assertThat(result.getData()).isNull();
    }
}
