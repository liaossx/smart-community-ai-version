package com.lsx.workorder.ai.docs;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class AiWorkOrderAssistantDocumentationTest {

    @Test
    void documentsDatabaseAndApiUsageForPhaseOne() throws Exception {
        String doc = readProjectFile("docs/ai-work-order-assistant.md");

        assertThat(doc).contains("biz_work_order_ai_analysis");
        assertThat(doc).contains("repair_id", "work_order_id", "category", "priority", "risk_level",
                "urgency_level", "recommended_team", "suggested_action", "summary",
                "extracted_location", "suggested_response_minutes", "safety_tips", "matched_keywords",
                "confidence", "manual_review_needed", "provider", "provider_version", "latest");

        assertThat(doc).contains("POST /api/workorder/ai/repair/{repairId}/analyze");
        assertThat(doc).contains("GET /api/workorder/ai/repair/{repairId}/latest");
        assertThat(doc).contains("GET /api/workorder/ai/repair/{repairId}/history");

        assertThat(doc).contains("multiple snapshots");
        assertThat(doc).contains("latest snapshot");
        assertThat(doc).contains("copy only `priority`");
        assertThat(doc).contains("does not assign");
        assertThat(doc).contains("rule-v1.1");
        assertThat(doc).contains("RoutingAiWorkOrderAnalysisProvider");
        assertThat(doc).contains("AI_WORKORDER_PROVIDER");
        assertThat(doc).contains("AI_SERVICE_URL");
        assertThat(doc).contains("SpringAiWorkOrderAnalysisProvider");
        assertThat(doc).contains("spring-ai");
        assertThat(doc).contains("canonical `house_id`");
        assertThat(doc).contains("rule-first provider");
        assertThat(doc).contains("future model provider");
        assertThat(doc).contains("ADR-0001", "ADR-0002");
    }

    private String readProjectFile(String relativePath) throws Exception {
        Path rootRelative = Paths.get(relativePath);
        Path moduleRelative = Paths.get("..").resolve(relativePath);
        Path path = Files.exists(rootRelative) ? rootRelative : moduleRelative;
        assertThat(path).exists();
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
