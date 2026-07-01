package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsMetricsSnapshot;
import com.lsx.ai.operations.dto.OperationsReportResponse;
import com.lsx.ai.operations.dto.OperationsRiskAlert;
import com.lsx.ai.observability.model.AiCallLogEntry;
import com.lsx.ai.observability.service.AiCallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpringAiOperationsReportAssistant implements OperationsReportAssistant {
    private static final Logger log = LoggerFactory.getLogger(SpringAiOperationsReportAssistant.class);

    /*
     * 运营周报的系统提示词。
     *
     * 这里约束模型只能基于输入指标分析，输出固定结构：
     * 周报摘要、亮点、风险提醒、居民诉求总结和行动建议。
     */
    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是智能社区后台运营分析助手。",
            "你的任务是根据输入的社区运营数据生成周报、风险提醒和居民诉求总结。",
            "只能基于输入数据分析，不要编造不存在的数量、事件、政策或工作人员。",
            "riskLevel 只能是 LOW, MEDIUM, HIGH, CRITICAL。",
            "如果存在高危维修、投诉积压、安全事件或明显异常，manualReviewNeeded 应为 true。",
            "输出面向物业管理人员，语言简洁、具体、可执行。",
            "返回结构化对象，字段包括 reportTitle、executiveSummary、weeklyHighlights、riskAlerts、residentAppealSummary、recommendedActions、manualReviewNeeded、confidence。"
    );

    private final ChatClient chatClient;
    private final OperationsReportNormalizer normalizer;
    private final AiCallLogService aiCallLogService;
    private final String providerVersion;
    private final String model;

    public SpringAiOperationsReportAssistant(ChatClient.Builder chatClientBuilder,
                                             OperationsReportNormalizer normalizer,
                                             AiCallLogService aiCallLogService,
                                             @Value("${smart-community.ai.operations.provider-version:operations-ai-v1}")
                                             String providerVersion,
                                             @Value("${spring.ai.openai.chat.options.model}")
                                             String model) {
        this.chatClient = chatClientBuilder.build();
        this.normalizer = normalizer;
        this.aiCallLogService = aiCallLogService;
        this.providerVersion = providerVersion;
        this.model = model;
    }

    @Override
    public OperationsReportResponse generateWeeklyReport(OperationsMetricsSnapshot request) {
        AiCallLogEntry callLog = AiCallLogEntry.start("OPERATIONS_REPORT")
                .bizKey("communityId=" + request.getCommunityId())
                .requestSummary(buildOperationData(request));
        try {
            // 把业务系统聚合好的运营指标封装成上下文，再让模型做总结。
            OperationsReportResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user -> user.text(String.join("\n",
                                    "请根据下面的社区运营数据生成结构化运营周报。",
                                    "{operationData}"))
                            .param("operationData", buildOperationData(request)))
                    .call()
                    .entity(OperationsReportResponse.class);
            // 模型输出后仍然要经过 normalizer，统一补字段、修正风险等级。
            OperationsReportResponse result =
                    normalizer.normalize(response, request, "SPRING_AI_OPERATIONS", providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("SUCCESS")
                    .confidence(result.getConfidence())
                    .responseSummary(result.getExecutiveSummary()));
            return result;
        } catch (RuntimeException ex) {
            log.warn("Operations report model call failed, use rule fallback. communityId={}",
                    request.getCommunityId(), ex);
            // 模型不可用时，用规则生成一份保底周报，不让后台运营页面空白。
            OperationsReportResponse result =
                    normalizer.normalize(ruleFallback(request), request, "RULE_OPERATIONS", providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("FALLBACK")
                    .confidence(result.getConfidence())
                    .responseSummary(result.getExecutiveSummary())
                    .error(ex));
            return result;
        }
    }

    private String buildOperationData(OperationsMetricsSnapshot request) {
        // 这里就是“运营数据上下文封装”，业务指标会原样进入 Prompt。
        return String.join("\n",
                "communityId: " + request.getCommunityId(),
                "communityName: " + request.getCommunityName(),
                "dateRange: " + request.getStartDate() + " 至 " + request.getEndDate(),
                "repairTotal: " + request.getRepairTotal(),
                "repairPending: " + request.getRepairPending(),
                "repairCompleted: " + request.getRepairCompleted(),
                "urgentRepairCount: " + request.getUrgentRepairCount(),
                "complaintTotal: " + request.getComplaintTotal(),
                "complaintPending: " + request.getComplaintPending(),
                "visitorTotal: " + request.getVisitorTotal(),
                "feeUnpaidCount: " + request.getFeeUnpaidCount(),
                "noticePublishedCount: " + request.getNoticePublishedCount(),
                "topRepairCategories: " + request.getTopRepairCategories(),
                "residentAppeals: " + request.getResidentAppeals(),
                "recentRiskEvents: " + request.getRecentRiskEvents()
        );
    }

    private OperationsReportResponse ruleFallback(OperationsMetricsSnapshot request) {
        // 保底版本只做确定性总结，不做复杂推理。
        OperationsReportResponse response = new OperationsReportResponse();
        response.setReportTitle(buildTitle(request));
        response.setExecutiveSummary("本期共收到报修" + defaultInt(request.getRepairTotal())
                + "单，投诉" + defaultInt(request.getComplaintTotal())
                + "件，待处理投诉" + defaultInt(request.getComplaintPending()) + "件。");
        response.setWeeklyHighlights(List.of(
                "维修完成数：" + defaultInt(request.getRepairCompleted()) + "单",
                "紧急报修数：" + defaultInt(request.getUrgentRepairCount()) + "单",
                "发布公告数：" + defaultInt(request.getNoticePublishedCount()) + "条"
        ));
        response.setResidentAppealSummary(buildAppealSummary(request));
        response.setRiskAlerts(buildRuleRisks(request));
        response.setRecommendedActions(List.of(
                "优先跟进未完成报修和待处理投诉。",
                "对高频诉求进行分类复盘，必要时发布说明公告。",
                "对风险事件安排人工复核并记录处理闭环。"
        ));
        response.setConfidence(65);
        return response;
    }

    private List<OperationsRiskAlert> buildRuleRisks(OperationsMetricsSnapshot request) {
        if (defaultInt(request.getUrgentRepairCount()) <= 0
                && defaultInt(request.getComplaintPending()) < 5
                && (request.getRecentRiskEvents() == null || request.getRecentRiskEvents().isEmpty())) {
            return List.of();
        }
        OperationsRiskAlert risk = new OperationsRiskAlert();
        risk.setRiskType("OPERATIONS_BACKLOG");
        risk.setRiskLevel(defaultInt(request.getUrgentRepairCount()) > 0 ? "HIGH" : "MEDIUM");
        risk.setDescription("存在紧急报修、投诉积压或近期风险事件，需要人工跟进。");
        risk.setSuggestedAction("安排管理员复核待处理事项，并在周会中确认责任人和完成时间。");
        return List.of(risk);
    }

    private String buildTitle(OperationsMetricsSnapshot request) {
        String communityName = request.getCommunityName() == null ? "社区" : request.getCommunityName();
        return communityName + "运营周报（" + request.getStartDate() + "至" + request.getEndDate() + "）";
    }

    private String buildAppealSummary(OperationsMetricsSnapshot request) {
        if (request.getResidentAppeals() == null || request.getResidentAppeals().isEmpty()) {
            return "本期未提供居民诉求明细。";
        }
        return "本期居民诉求主要集中在：" + String.join("；", request.getResidentAppeals()) + "。";
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}

