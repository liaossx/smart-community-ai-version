package com.lsx.ai.workorder.service;

import com.lsx.ai.observability.model.AiCallLogEntry;
import com.lsx.ai.observability.service.AiCallLogService;
import com.lsx.ai.workorder.dto.WorkOrderAnalyzeRequest;
import com.lsx.ai.workorder.dto.WorkOrderAnalyzeResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SpringAiWorkOrderAnalyzer implements WorkOrderAiAnalyzer {
    /*
     * 工单分析的大模型系统提示词。
     *
     * 它约束模型只输出后端可接收的枚举和字段：
     * category、priority、urgencyLevel、riskLevel 等。
     */
    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是智能社区工单分析助手。",
            "你的任务是根据业主报修内容，输出可直接给后端保存的结构化工单分析结果。",
            "必须遵守以下枚举：",
            "category 只能是 WATER, ELECTRICAL, ELEVATOR, PUBLIC_FACILITY, BUILDING, OTHER。",
            "priority 只能是 1, 2, 3, 4；1=普通，2=较急，3=紧急，4=高危特急。",
            "urgencyLevel 只能是 LOW, MEDIUM, HIGH, CRITICAL。",
            "riskLevel 只能是 LOW, MEDIUM, HIGH。",
            "recommendedTeam, suggestedAction, summary, safetyTips 使用中文。",
            "如果存在电梯困人、起火、冒烟、燃气、漏电、爆管、大面积积水、无法进出等风险，priority 应为 4，manualReviewNeeded 应为 true。",
            "不要编造具体维修人员，不要输出 workerId、workerName、workerPhone。"
    );

    private final ChatClient chatClient;
    private final WorkOrderAnalysisNormalizer normalizer;
    private final AiCallLogService aiCallLogService;
    private final String providerVersion;
    private final String model;

    public SpringAiWorkOrderAnalyzer(ChatClient.Builder chatClientBuilder,
                                     WorkOrderAnalysisNormalizer normalizer,
                                     AiCallLogService aiCallLogService,
                                     @Value("${smart-community.ai.workorder.provider-version:spring-ai-v1}")
                                     String providerVersion,
                                     @Value("${spring.ai.openai.chat.options.model:unknown}")
                                     String model) {
        this.chatClient = chatClientBuilder.build();
        this.normalizer = normalizer;
        this.aiCallLogService = aiCallLogService;
        this.providerVersion = providerVersion;
        this.model = model;
    }

    @Override
    public WorkOrderAnalyzeResponse analyze(WorkOrderAnalyzeRequest request) {
        AiCallLogEntry callLog = AiCallLogEntry.start("WORKORDER_ANALYZE")
                .bizKey(request.getRepairId() == null ? null : "repairId=" + request.getRepairId())
                .requestSummary("faultType=" + request.getFaultType() + "; faultDesc=" + request.getFaultDesc());
        try {
            // 封装“当前报修单上下文”，让模型基于 faultType/faultDesc 做分类和提取。
            WorkOrderAnalyzeResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user -> user.text(String.join("\n",
                                    "请分析下面的社区报修信息，并返回结构化对象。",
                                    "repairId: {repairId}",
                                    "faultType: {faultType}",
                                    "faultDesc: {faultDesc}"))
                            .param("repairId", request.getRepairId())
                            .param("faultType", request.getFaultType())
                            .param("faultDesc", request.getFaultDesc()))
                    .call()
                    .entity(WorkOrderAnalyzeResponse.class);

            // 模型输出后再走业务规则校正，保证 priority、风险等级等字段稳定。
            WorkOrderAnalyzeResponse result = normalizer.normalize(response, providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("SUCCESS")
                    .confidence(result.getConfidence())
                    .responseSummary(result.getSummary()));
            return result;
        } catch (RuntimeException ex) {
            aiCallLogService.record(callLog
                    .provider("SPRING_AI", providerVersion, model)
                    .status("FAILED")
                    .error(ex));
            throw ex;
        }
    }
}
