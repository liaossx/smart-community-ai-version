package com.lsx.workorder.ai.provider;

import com.lsx.workorder.ai.config.AiWorkOrderProviderProperties;
import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.ai.provider.impl.RoutingAiWorkOrderAnalysisProvider;
import com.lsx.workorder.repair.entity.Repair;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingAiWorkOrderAnalysisProviderTest {

    @Test
    void routesToRuleProviderByDefault() {
        AiWorkOrderProviderProperties properties = new AiWorkOrderProviderProperties();
        AtomicInteger calls = new AtomicInteger();
        AiWorkOrderAnalysisProvider ruleProvider = repair -> {
            calls.incrementAndGet();
            return result("RULE");
        };
        AiWorkOrderAnalysisProvider springAiProvider = repair -> result("SPRING_AI");
        RoutingAiWorkOrderAnalysisProvider provider =
                new RoutingAiWorkOrderAnalysisProvider(properties, ruleProvider, springAiProvider);

        AiWorkOrderAnalyzeResult result = provider.analyze(new Repair());

        assertThat(result.getProvider()).isEqualTo("RULE");
        assertThat(calls).hasValue(1);
    }

    @Test
    void routesToRuleProviderWhenRuleIsConfiguredWithExtraWhitespace() {
        AiWorkOrderProviderProperties properties = new AiWorkOrderProviderProperties();
        properties.setProvider(" RULE ");
        AtomicInteger calls = new AtomicInteger();
        AiWorkOrderAnalysisProvider ruleProvider = repair -> {
            calls.incrementAndGet();
            return result("RULE");
        };
        AiWorkOrderAnalysisProvider springAiProvider = repair -> result("SPRING_AI");
        RoutingAiWorkOrderAnalysisProvider provider =
                new RoutingAiWorkOrderAnalysisProvider(properties, ruleProvider, springAiProvider);

        AiWorkOrderAnalyzeResult result = provider.analyze(new Repair());

        assertThat(result.getProvider()).isEqualTo("RULE");
        assertThat(calls).hasValue(1);
    }

    @Test
    void routesToSpringAiProviderWhenSpringAiIsConfigured() {
        AiWorkOrderProviderProperties properties = new AiWorkOrderProviderProperties();
        properties.setProvider("spring-ai");
        AtomicInteger ruleCalls = new AtomicInteger();
        AtomicInteger springAiCalls = new AtomicInteger();
        AiWorkOrderAnalysisProvider ruleProvider = repair -> {
            ruleCalls.incrementAndGet();
            return result("RULE");
        };
        AiWorkOrderAnalysisProvider springAiProvider = repair -> {
            springAiCalls.incrementAndGet();
            return result("SPRING_AI");
        };
        RoutingAiWorkOrderAnalysisProvider provider =
                new RoutingAiWorkOrderAnalysisProvider(properties, ruleProvider, springAiProvider);

        AiWorkOrderAnalyzeResult result = provider.analyze(new Repair());

        assertThat(result.getProvider()).isEqualTo("SPRING_AI");
        assertThat(ruleCalls).hasValue(0);
        assertThat(springAiCalls).hasValue(1);
    }

    @Test
    void fallsBackToRuleProviderWhenSpringAiProviderFails() {
        AiWorkOrderProviderProperties properties = new AiWorkOrderProviderProperties();
        properties.setProvider("spring-ai");
        AtomicInteger ruleCalls = new AtomicInteger();
        AiWorkOrderAnalysisProvider ruleProvider = repair -> {
            ruleCalls.incrementAndGet();
            return result("RULE");
        };
        AiWorkOrderAnalysisProvider springAiProvider = repair -> {
            throw new RuntimeException("ai-service unavailable");
        };
        RoutingAiWorkOrderAnalysisProvider provider =
                new RoutingAiWorkOrderAnalysisProvider(properties, ruleProvider, springAiProvider);

        AiWorkOrderAnalyzeResult result = provider.analyze(new Repair());

        assertThat(result.getProvider()).isEqualTo("RULE");
        assertThat(ruleCalls).hasValue(1);
    }

    private AiWorkOrderAnalyzeResult result(String provider) {
        AiWorkOrderAnalyzeResult result = new AiWorkOrderAnalyzeResult();
        result.setProvider(provider);
        return result;
    }
}
