package com.lsx.workorder.ai.service;

import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.ai.entity.AiWorkOrderAnalysis;
import com.lsx.workorder.ai.provider.AiWorkOrderAnalysisProvider;
import com.lsx.workorder.ai.repository.AiWorkOrderAnalysisStore;
import com.lsx.workorder.ai.service.impl.AiWorkOrderAnalysisServiceImpl;
import com.lsx.workorder.repair.entity.Repair;
import com.lsx.workorder.repair.service.RepairService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiWorkOrderAnalysisServiceTest {

    @Mock
    private RepairService repairService;

    @Test
    void analyzeRepairCreatesLatestSnapshot() {
        Repair repair = new Repair();
        repair.setId(100L);
        repair.setFaultType("water");
        repair.setFaultDesc("pipe leak in bathroom");

        when(repairService.getById(100L)).thenReturn(repair);

        InMemoryAiWorkOrderAnalysisStore store = new InMemoryAiWorkOrderAnalysisStore();
        AiWorkOrderAnalysisProvider provider = ignored -> sampleResult();
        AiWorkOrderAnalysisService service = new AiWorkOrderAnalysisServiceImpl(repairService, provider, store);

        AiWorkOrderAnalysis snapshot = service.analyzeRepair(100L);

        assertThat(snapshot.getRepairId()).isEqualTo(100L);
        assertThat(snapshot.getCategory()).isEqualTo("WATER");
        assertThat(snapshot.getPriority()).isEqualTo(3);
        assertThat(snapshot.getUrgencyLevel()).isEqualTo("HIGH");
        assertThat(snapshot.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(snapshot.getRecommendedTeam()).isEqualTo("\u6c34\u6696\u7ef4\u4fee\u7ec4");
        assertThat(snapshot.getSuggestedAction()).isEqualTo("\u5efa\u8bae\u5148\u8054\u7cfb\u4f4f\u6237\u786e\u8ba4\u6f0f\u6c34\u8303\u56f4\u3002");
        assertThat(snapshot.getSummary()).isEqualTo("\u536b\u751f\u95f4\u6c34\u7ba1\u6f0f\u6c34");
        assertThat(snapshot.getExtractedLocation()).isEqualTo("3\u680b2\u5355\u51431801\u536b\u751f\u95f4");
        assertThat(snapshot.getSuggestedResponseMinutes()).isEqualTo(30);
        assertThat(snapshot.getSafetyTips()).contains("\u5173\u95ed\u9600\u95e8", "\u79fb\u5f00\u7535\u5668");
        assertThat(snapshot.getMatchedKeywords()).isEqualTo("pipe,leak");
        assertThat(snapshot.getConfidence()).isEqualTo(90);
        assertThat(snapshot.getManualReviewNeeded()).isFalse();
        assertThat(snapshot.getProvider()).isEqualTo("RULE");
        assertThat(snapshot.getProviderVersion()).isEqualTo("rule-v1.1");
        assertThat(snapshot.getLatest()).isTrue();
        assertThat(store.findLatestByRepairId(100L)).containsSame(snapshot);
    }

    @Test
    void analyzeRepairCreatesNewSnapshotAndReplacesLatest() {
        Repair repair = new Repair();
        repair.setId(100L);
        repair.setFaultType("water");
        repair.setFaultDesc("pipe leak in bathroom");

        when(repairService.getById(100L)).thenReturn(repair);

        InMemoryAiWorkOrderAnalysisStore store = new InMemoryAiWorkOrderAnalysisStore();
        AiWorkOrderAnalysisProvider provider = ignored -> sampleResult();
        AiWorkOrderAnalysisService service = new AiWorkOrderAnalysisServiceImpl(repairService, provider, store);

        AiWorkOrderAnalysis first = service.analyzeRepair(100L);
        AiWorkOrderAnalysis second = service.analyzeRepair(100L);

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getLatest()).isFalse();
        assertThat(second.getLatest()).isTrue();
        assertThat(store.findHistoryByRepairId(100L)).containsExactly(second, first);
        assertThat(store.findLatestByRepairId(100L)).containsSame(second);
    }

    @Test
    void analyzeRepairFailsWhenRepairReportDoesNotExist() {
        when(repairService.getById(404L)).thenReturn(null);

        InMemoryAiWorkOrderAnalysisStore store = new InMemoryAiWorkOrderAnalysisStore();
        AiWorkOrderAnalysisProvider provider = ignored -> sampleResult();
        AiWorkOrderAnalysisService service = new AiWorkOrderAnalysisServiceImpl(repairService, provider, store);

        assertThatThrownBy(() -> service.analyzeRepair(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Repair report not found");

        assertThat(store.findHistoryByRepairId(404L)).isEmpty();
        assertThat(store.findLatestByRepairId(404L)).isEmpty();
    }

    @Test
    void getLatestAnalysisReturnsLatestSnapshotWithoutCreatingANewOne() {
        Repair repair = new Repair();
        repair.setId(100L);

        when(repairService.getById(100L)).thenReturn(repair);

        InMemoryAiWorkOrderAnalysisStore store = new InMemoryAiWorkOrderAnalysisStore();
        AiWorkOrderAnalysis older = savedSnapshot(store, 100L, false, "OTHER");
        AiWorkOrderAnalysis latest = savedSnapshot(store, 100L, true, "WATER");
        AiWorkOrderAnalysisProvider provider = ignored -> sampleResult();
        AiWorkOrderAnalysisService service = new AiWorkOrderAnalysisServiceImpl(repairService, provider, store);

        AiWorkOrderAnalysis result = service.getLatestAnalysis(100L);

        assertThat(result).isSameAs(latest);
        assertThat(result.getCategory()).isEqualTo("WATER");
        assertThat(store.findHistoryByRepairId(100L)).containsExactly(latest, older);
    }

    @Test
    void getLatestAnalysisFailsWhenRepairReportDoesNotExist() {
        when(repairService.getById(404L)).thenReturn(null);

        InMemoryAiWorkOrderAnalysisStore store = new InMemoryAiWorkOrderAnalysisStore();
        AiWorkOrderAnalysisProvider provider = ignored -> sampleResult();
        AiWorkOrderAnalysisService service = new AiWorkOrderAnalysisServiceImpl(repairService, provider, store);

        assertThatThrownBy(() -> service.getLatestAnalysis(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Repair report not found");
    }

    @Test
    void getLatestAnalysisFailsWhenRepairReportHasNoSnapshot() {
        Repair repair = new Repair();
        repair.setId(100L);

        when(repairService.getById(100L)).thenReturn(repair);

        InMemoryAiWorkOrderAnalysisStore store = new InMemoryAiWorkOrderAnalysisStore();
        AiWorkOrderAnalysisProvider provider = ignored -> sampleResult();
        AiWorkOrderAnalysisService service = new AiWorkOrderAnalysisServiceImpl(repairService, provider, store);

        assertThatThrownBy(() -> service.getLatestAnalysis(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AI analysis snapshot not found");

        assertThat(store.findHistoryByRepairId(100L)).isEmpty();
    }

    @Test
    void getAnalysisHistoryReturnsSnapshotsNewestFirstWithoutChangingThem() {
        Repair repair = new Repair();
        repair.setId(100L);

        when(repairService.getById(100L)).thenReturn(repair);

        InMemoryAiWorkOrderAnalysisStore store = new InMemoryAiWorkOrderAnalysisStore();
        AiWorkOrderAnalysis older = savedSnapshot(store, 100L, false, "OTHER");
        AiWorkOrderAnalysis latest = savedSnapshot(store, 100L, true, "WATER");
        AiWorkOrderAnalysisProvider provider = ignored -> sampleResult();
        AiWorkOrderAnalysisService service = new AiWorkOrderAnalysisServiceImpl(repairService, provider, store);

        List<AiWorkOrderAnalysis> history = service.getAnalysisHistory(100L);

        assertThat(history).containsExactly(latest, older);
        assertThat(history.get(0).getLatest()).isTrue();
        assertThat(history.get(1).getLatest()).isFalse();
        assertThat(store.findHistoryByRepairId(100L)).containsExactly(latest, older);
    }

    @Test
    void getAnalysisHistoryReturnsEmptyListWhenRepairReportHasNoSnapshots() {
        Repair repair = new Repair();
        repair.setId(100L);

        when(repairService.getById(100L)).thenReturn(repair);

        InMemoryAiWorkOrderAnalysisStore store = new InMemoryAiWorkOrderAnalysisStore();
        AiWorkOrderAnalysisProvider provider = ignored -> sampleResult();
        AiWorkOrderAnalysisService service = new AiWorkOrderAnalysisServiceImpl(repairService, provider, store);

        List<AiWorkOrderAnalysis> history = service.getAnalysisHistory(100L);

        assertThat(history).isEmpty();
    }

    @Test
    void getAnalysisHistoryFailsWhenRepairReportDoesNotExist() {
        when(repairService.getById(404L)).thenReturn(null);

        InMemoryAiWorkOrderAnalysisStore store = new InMemoryAiWorkOrderAnalysisStore();
        AiWorkOrderAnalysisProvider provider = ignored -> sampleResult();
        AiWorkOrderAnalysisService service = new AiWorkOrderAnalysisServiceImpl(repairService, provider, store);

        assertThatThrownBy(() -> service.getAnalysisHistory(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Repair report not found");
    }

    private AiWorkOrderAnalyzeResult sampleResult() {
        AiWorkOrderAnalyzeResult result = new AiWorkOrderAnalyzeResult();
        result.setCategory("WATER");
        result.setPriority(3);
        result.setUrgencyLevel("HIGH");
        result.setRiskLevel("MEDIUM");
        result.setRecommendedTeam("\u6c34\u6696\u7ef4\u4fee\u7ec4");
        result.setSuggestedAction("\u5efa\u8bae\u5148\u8054\u7cfb\u4f4f\u6237\u786e\u8ba4\u6f0f\u6c34\u8303\u56f4\u3002");
        result.setSummary("\u536b\u751f\u95f4\u6c34\u7ba1\u6f0f\u6c34");
        result.setExtractedLocation("3\u680b2\u5355\u51431801\u536b\u751f\u95f4");
        result.setSuggestedResponseMinutes(30);
        result.setSafetyTips(Arrays.asList("\u5173\u95ed\u9600\u95e8", "\u79fb\u5f00\u7535\u5668"));
        result.setMatchedKeywords(Arrays.asList("pipe", "leak"));
        result.setConfidence(90);
        result.setManualReviewNeeded(false);
        result.setProvider("RULE");
        result.setProviderVersion("rule-v1.1");
        return result;
    }

    private AiWorkOrderAnalysis savedSnapshot(InMemoryAiWorkOrderAnalysisStore store,
                                              Long repairId,
                                              Boolean latest,
                                              String category) {
        AiWorkOrderAnalysis snapshot = new AiWorkOrderAnalysis();
        snapshot.setRepairId(repairId);
        snapshot.setLatest(latest);
        snapshot.setCategory(category);
        snapshot.setCreateTime(java.time.LocalDateTime.now().plusSeconds(store.nextId));
        return store.save(snapshot);
    }

    private static class InMemoryAiWorkOrderAnalysisStore implements AiWorkOrderAnalysisStore {
        private final List<AiWorkOrderAnalysis> snapshots = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public void clearLatest(Long repairId) {
            snapshots.stream()
                    .filter(snapshot -> repairId.equals(snapshot.getRepairId()))
                    .forEach(snapshot -> snapshot.setLatest(false));
        }

        @Override
        public AiWorkOrderAnalysis save(AiWorkOrderAnalysis analysis) {
            analysis.setId(nextId++);
            snapshots.add(analysis);
            return analysis;
        }

        @Override
        public Optional<AiWorkOrderAnalysis> findLatestByRepairId(Long repairId) {
            return snapshots.stream()
                    .filter(snapshot -> repairId.equals(snapshot.getRepairId()))
                    .filter(AiWorkOrderAnalysis::getLatest)
                    .findFirst();
        }

        @Override
        public List<AiWorkOrderAnalysis> findHistoryByRepairId(Long repairId) {
            List<AiWorkOrderAnalysis> history = new ArrayList<>();
            snapshots.stream()
                    .filter(snapshot -> repairId.equals(snapshot.getRepairId()))
                    .forEach(history::add);
            history.sort((left, right) -> {
                int byCreateTime = right.getCreateTime().compareTo(left.getCreateTime());
                if (byCreateTime != 0) {
                    return byCreateTime;
                }
                return Long.compare(right.getId(), left.getId());
            });
            return history;
        }
    }
}
