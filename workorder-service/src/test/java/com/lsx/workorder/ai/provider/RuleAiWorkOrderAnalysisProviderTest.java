package com.lsx.workorder.ai.provider;

import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.ai.provider.impl.RuleAiWorkOrderAnalysisProvider;
import com.lsx.workorder.repair.entity.Repair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RuleAiWorkOrderAnalysisProviderTest {

    @Test
    void analyzesChineseWaterLeakRepairReportWithLocationAndDisplayFields() {
        Repair repair = new Repair();
        repair.setFaultType("\u6f0f\u6c34");
        repair.setFaultDesc("3\u680b2\u5355\u51431801\u53a8\u623f\u6c34\u69fd\u4e0b\u65b9\u6f0f\u6c34\uff0c\u6c34\u5df2\u7ecf\u6d41\u5230\u5ba2\u5385\u3002");

        AiWorkOrderAnalysisProvider provider = new RuleAiWorkOrderAnalysisProvider();

        AiWorkOrderAnalyzeResult result = provider.analyze(repair);

        assertThat(result.getCategory()).isEqualTo("WATER");
        assertThat(result.getPriority()).isEqualTo(3);
        assertThat(result.getPriorityDesc()).isEqualTo("\u7d27\u6025");
        assertThat(result.getUrgencyLevel()).isEqualTo("HIGH");
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(result.getRecommendedTeam()).isEqualTo("\u6c34\u6696\u7ef4\u4fee\u7ec4");
        assertThat(result.getExtractedLocation()).isEqualTo("3\u680b2\u5355\u51431801\u53a8\u623f\u6c34\u69fd\u4e0b\u65b9");
        assertThat(result.getSuggestedResponseMinutes()).isEqualTo(30);
        assertThat(result.getSuggestedAction()).contains("\u9600\u95e8");
        assertThat(result.getSummary()).contains("\u63d0\u53d6\u4f4d\u7f6e");
        assertThat(result.getMatchedKeywords()).contains("\u6f0f\u6c34", "\u6c34\u69fd");
        assertThat(result.getSafetyTips()).isNotEmpty();
        assertThat(result.getConfidence()).isGreaterThanOrEqualTo(80);
        assertThat(result.getManualReviewNeeded()).isFalse();
        assertThat(result.getProvider()).isEqualTo("RULE");
        assertThat(result.getProviderVersion()).isEqualTo("rule-v1.1");
    }

    @ParameterizedTest
    @CsvSource({
            "'\u7535\u68af\u56f0\u4eba\uff0c\u4f4f\u6237\u88ab\u56f0\u5728\u8f7f\u53a2\u91cc\u3002', ELEVATOR, '\u7535\u68af\u7ef4\u4fdd\u7ec4'",
            "'\u897f\u95e8\u95e8\u7981\u6253\u4e0d\u5f00\uff0c\u8001\u4eba\u65e0\u6cd5\u8fdb\u51fa\u3002', PUBLIC_FACILITY, '\u516c\u5171\u8bbe\u65bd\u7ef4\u4fee\u7ec4'",
            "'\u697c\u9053\u5899\u9762\u8131\u843d\uff0c\u5730\u7816\u88c2\u7f1d\u660e\u663e\u3002', BUILDING, '\u571f\u5efa\u7ef4\u4fee\u7ec4'"
    })
    void classifiesCommonChineseRepairReportCategories(String faultDesc,
                                                       String expectedCategory,
                                                       String expectedTeam) {
        Repair repair = new Repair();
        repair.setFaultDesc(faultDesc);

        AiWorkOrderAnalysisProvider provider = new RuleAiWorkOrderAnalysisProvider();

        AiWorkOrderAnalyzeResult result = provider.analyze(repair);

        assertThat(result.getCategory()).isEqualTo(expectedCategory);
        assertThat(result.getRecommendedTeam()).isEqualTo(expectedTeam);
        assertThat(result.getMatchedKeywords()).isNotEmpty();
        assertThat(result.getSafetyTips()).isNotEmpty();
        assertThat(result.getProvider()).isEqualTo("RULE");
        assertThat(result.getProviderVersion()).isEqualTo("rule-v1.1");
    }

    @Test
    void marksHighRiskRepairReportsForManualReview() {
        Repair repair = new Repair();
        repair.setFaultDesc("\u7535\u68af\u56f0\u4eba\uff0c\u4f4f\u6237\u88ab\u56f0\u5728\u8f7f\u53a2\u91cc\uff0c\u73b0\u573a\u6709\u5192\u70df\u3002");

        AiWorkOrderAnalysisProvider provider = new RuleAiWorkOrderAnalysisProvider();

        AiWorkOrderAnalyzeResult result = provider.analyze(repair);

        assertThat(result.getCategory()).isEqualTo("ELEVATOR");
        assertThat(result.getPriority()).isEqualTo(4);
        assertThat(result.getPriorityDesc()).isEqualTo("\u9ad8\u5371\u7279\u6025");
        assertThat(result.getUrgencyLevel()).isEqualTo("CRITICAL");
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
        assertThat(result.getSuggestedResponseMinutes()).isEqualTo(15);
        assertThat(result.getManualReviewNeeded()).isTrue();
        assertThat(result.getMatchedKeywords()).contains("\u56f0\u4eba", "\u5192\u70df");
        assertThat(result.getConfidence()).isGreaterThanOrEqualTo(90);
        assertThat(result.getSuggestedAction()).contains("\u9ad8\u5371\u7279\u6025");
    }

    @Test
    void usesBusinessRiskPriorityWhenMultipleCategoriesMatch() {
        Repair repair = new Repair();
        repair.setFaultDesc("\u536b\u751f\u95f4\u6f0f\u6c34\uff0c\u63d2\u5ea7\u9644\u8fd1\u7591\u4f3c\u6f0f\u7535\uff0c\u8fd8\u6709\u706b\u82b1\u3002");

        AiWorkOrderAnalysisProvider provider = new RuleAiWorkOrderAnalysisProvider();

        AiWorkOrderAnalyzeResult result = provider.analyze(repair);

        assertThat(result.getCategory()).isEqualTo("ELECTRICAL");
        assertThat(result.getPriority()).isEqualTo(4);
        assertThat(result.getUrgencyLevel()).isEqualTo("CRITICAL");
        assertThat(result.getRecommendedTeam()).isEqualTo("\u7535\u5de5\u7ef4\u4fee\u7ec4");
        assertThat(result.getManualReviewNeeded()).isTrue();
        assertThat(result.getMatchedKeywords()).contains("\u6f0f\u6c34", "\u63d2\u5ea7", "\u6f0f\u7535", "\u706b\u82b1");
        assertThat(result.getSuggestedAction()).contains("\u591a\u4e2a\u4e13\u4e1a");
        assertThat(result.getSafetyTips()).anyMatch(tip -> tip.contains("\u4eba\u5de5\u590d\u6838"));
    }

    @Test
    void treatsElevatorEntranceWaterAsWaterCategoryAndExtractsPublicLocation() {
        Repair repair = new Repair();
        repair.setFaultDesc("\u5730\u4e0b\u8f66\u5e93B\u533a\u7535\u68af\u53e3\u6709\u79ef\u6c34\uff0c\u5f71\u54cd\u901a\u884c\u3002");

        AiWorkOrderAnalysisProvider provider = new RuleAiWorkOrderAnalysisProvider();

        AiWorkOrderAnalyzeResult result = provider.analyze(repair);

        assertThat(result.getCategory()).isEqualTo("WATER");
        assertThat(result.getExtractedLocation()).isEqualTo("\u5730\u4e0b\u8f66\u5e93B\u533a\u7535\u68af\u53e3");
        assertThat(result.getMatchedKeywords()).contains("\u79ef\u6c34");
        assertThat(result.getManualReviewNeeded()).isFalse();
    }

    @Test
    void lowersConfidenceAndRequiresReviewForUncertainReports() {
        Repair repair = new Repair();
        repair.setFaultType("\u6f0f\u6c34");
        repair.setFaultDesc("\u53ef\u80fd\u662f\u6c34\u7ba1\u95ee\u9898\uff0c\u4f46\u4f4f\u6237\u4e0d\u786e\u5b9a\u3002");

        AiWorkOrderAnalysisProvider provider = new RuleAiWorkOrderAnalysisProvider();

        AiWorkOrderAnalyzeResult result = provider.analyze(repair);

        assertThat(result.getCategory()).isEqualTo("WATER");
        assertThat(result.getMatchedKeywords()).contains("\u6f0f\u6c34", "\u53ef\u80fd", "\u4e0d\u786e\u5b9a", "\u6c34\u7ba1");
        assertThat(result.getConfidence()).isBetween(55, 75);
        assertThat(result.getManualReviewNeeded()).isTrue();
    }

    @Test
    void returnsSafeFallbackForMissingRepairText() {
        AiWorkOrderAnalysisProvider provider = new RuleAiWorkOrderAnalysisProvider();

        AiWorkOrderAnalyzeResult result = provider.analyze(null);

        assertThat(result.getCategory()).isEqualTo("OTHER");
        assertThat(result.getPriority()).isEqualTo(1);
        assertThat(result.getPriorityDesc()).isEqualTo("\u666e\u901a");
        assertThat(result.getUrgencyLevel()).isEqualTo("LOW");
        assertThat(result.getRiskLevel()).isEqualTo("LOW");
        assertThat(result.getRecommendedTeam()).isEqualTo("\u7efc\u5408\u7ef4\u4fee\u7ec4");
        assertThat(result.getSuggestedResponseMinutes()).isEqualTo(120);
        assertThat(result.getMatchedKeywords()).isEmpty();
        assertThat(result.getSafetyTips()).isNotEmpty();
        assertThat(result.getConfidence()).isLessThanOrEqualTo(50);
        assertThat(result.getManualReviewNeeded()).isTrue();
        assertThat(result.getProvider()).isEqualTo("RULE");
        assertThat(result.getProviderVersion()).isEqualTo("rule-v1.1");
    }
}
