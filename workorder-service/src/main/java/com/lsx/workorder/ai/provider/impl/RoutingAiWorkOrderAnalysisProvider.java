package com.lsx.workorder.ai.provider.impl;

import com.lsx.workorder.ai.config.AiWorkOrderProviderProperties;
import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.ai.provider.AiWorkOrderAnalysisProvider;
import com.lsx.workorder.repair.entity.Repair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Primary
@Component
public class RoutingAiWorkOrderAnalysisProvider implements AiWorkOrderAnalysisProvider {
    /*
     * 工单 AI Provider 路由层。
     *
     * workorder-service 不直接依赖某个模型，而是通过配置切换：
     * rule      -> 本地规则分析
     * spring-ai -> 远程 ai-service / DeepSeek 分析
     */
    private static final String RULE_PROVIDER = "rule";
    private static final String SPRING_AI_PROVIDER = "spring-ai";

    private final AiWorkOrderProviderProperties properties;
    private final AiWorkOrderAnalysisProvider ruleProvider;
    private final AiWorkOrderAnalysisProvider springAiProvider;

    public RoutingAiWorkOrderAnalysisProvider(AiWorkOrderProviderProperties properties,
                                              @Qualifier("ruleAiWorkOrderAnalysisProvider")
                                              AiWorkOrderAnalysisProvider ruleProvider,
                                              @Qualifier("springAiWorkOrderAnalysisProvider")
                                              AiWorkOrderAnalysisProvider springAiProvider) {
        this.properties = properties;
        this.ruleProvider = ruleProvider;
        this.springAiProvider = springAiProvider;
    }

    @Override
    public AiWorkOrderAnalyzeResult analyze(Repair repair) {
        // 运行时读取配置，决定当前工单分析走规则还是走大模型。
        String provider = normalizeProvider(properties.getProvider());
        if (RULE_PROVIDER.equals(provider)) {
            return ruleProvider.analyze(repair);
        }
        if (SPRING_AI_PROVIDER.equals(provider)) {
            return analyzeWithSpringAiFallback(repair);
        }
        log.warn("Unknown AI work order provider '{}'; falling back to rule provider", properties.getProvider());
        return ruleProvider.analyze(repair);
    }

    private AiWorkOrderAnalyzeResult analyzeWithSpringAiFallback(Repair repair) {
        try {
            AiWorkOrderAnalyzeResult result = springAiProvider.analyze(repair);
            if (result != null) {
                return result;
            }
            log.warn("Spring AI work order provider returned null; falling back to rule provider");
        } catch (Exception e) {
            log.warn("Spring AI work order provider failed; falling back to rule provider", e);
        }
        // 企业级关键点：AI 失败不能影响主业务，降级回规则 Provider。
        return ruleProvider.analyze(repair);
    }

    private String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            return RULE_PROVIDER;
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
