package com.lsx.ai.operations.service;

import com.lsx.ai.observability.model.AiCallLogEntry;
import com.lsx.ai.observability.service.AiCallLogService;
import com.lsx.ai.operations.dto.OperationsActionItem;
import com.lsx.ai.operations.dto.OperationsInsightCard;
import com.lsx.ai.operations.dto.OperationsInsightsResponse;
import com.lsx.ai.operations.dto.OperationsMetricsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class SpringAiOperationsInsightsAssistant implements OperationsInsightsAssistant {
    private static final Logger log = LoggerFactory.getLogger(SpringAiOperationsInsightsAssistant.class);

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是智能社区后台的 AI 运营数据分析助手。",
            "你的任务不是复述报表，而是基于输入指标发现运营风险、异常信号、居民诉求和可执行动作。",
            "只能基于输入数据分析，不要编造不存在的事件、政策、人员或精确数量。",
            "输出要适合 Java 后端结构化解析，并直接用于运营驾驶舱展示。",
            "overallRiskLevel 和 insightCards.riskLevel 只能是 LOW, MEDIUM, HIGH, CRITICAL。",
            "actionItems.priority 只能是 P0, P1, P2, P3。",
            "每张 insightCard 必须包含 evidence，说明它来自哪些输入指标或风险事件。",
            "如果存在紧急报修、投诉积压、安全风险事件或欠费压力，manualReviewNeeded 应为 true。",
            "返回结构化对象，字段包含 overallRiskLevel、headline、summary、insightCards、actionItems、manualReviewNeeded、confidence。"
    );

    private final ChatClient chatClient;
    private final OperationsInsightsNormalizer normalizer;
    private final AiCallLogService aiCallLogService;
    private final String providerVersion;
    private final String model;

    public SpringAiOperationsInsightsAssistant(ChatClient.Builder chatClientBuilder,
                                               OperationsInsightsNormalizer normalizer,
                                               AiCallLogService aiCallLogService,
                                               @Value("${smart-community.ai.operations.provider-version:operations-ai-v1}")
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
    public OperationsInsightsResponse generateInsights(OperationsMetricsSnapshot request) {
        AiCallLogEntry callLog = AiCallLogEntry.start("OPERATIONS_INSIGHTS")
                .bizKey("communityId=" + request.getCommunityId())
                .requestSummary(buildOperationData(request));
        try {
            OperationsInsightsResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user -> user.text(String.join("\n",
                                    "请基于下面的社区运营指标，生成结构化 AI 运营洞察。",
                                    "重点分析：报修热点、投诉风险、欠费压力、近期风险事件、需要人工复核的事项。",
                                    "{operationData}"))
                            .param("operationData", buildOperationData(request)))
                    .call()
                    .entity(OperationsInsightsResponse.class);
            OperationsInsightsResponse result = normalizer.normalize(
                    response, request, "SPRING_AI_OPERATIONS_INSIGHTS", providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("SUCCESS")
                    .confidence(result.getConfidence())
                    .responseSummary(result.getHeadline()));
            return result;
        } catch (RuntimeException ex) {
            log.warn("Operations insights model call failed, use rule fallback. communityId={}",
                    request.getCommunityId(), ex);
            OperationsInsightsResponse result = normalizer.normalize(
                    ruleFallback(request), request, "RULE_OPERATIONS_INSIGHTS", providerVersion, model);
            aiCallLogService.record(callLog
                    .provider(result.getProvider(), result.getProviderVersion(), result.getModel())
                    .status("FALLBACK")
                    .confidence(result.getConfidence())
                    .responseSummary(result.getHeadline())
                    .error(ex));
            return result;
        }
    }

    private OperationsInsightsResponse ruleFallback(OperationsMetricsSnapshot request) {
        OperationsInsightsResponse response = new OperationsInsightsResponse();
        response.setHeadline(buildFallbackHeadline(request));
        response.setSummary("已基于业务库指标完成运营风险识别，建议优先处理高风险事项。");
        response.setInsightCards(buildRuleCards(request));
        response.setActionItems(buildRuleActions(request));
        response.setConfidence(72);
        return response;
    }

    private List<OperationsInsightCard> buildRuleCards(OperationsMetricsSnapshot request) {
        List<OperationsInsightCard> cards = new ArrayList<>();
        if (defaultInt(request.getUrgentRepairCount()) > 0 || !isEmpty(request.getRecentRiskEvents())) {
            cards.add(card(
                    "RISK_EVENT",
                    defaultInt(request.getUrgentRepairCount()) >= 3 ? "CRITICAL" : "HIGH",
                    "近期存在需要优先处置的安全或紧急事件",
                    List.of("urgentRepairCount=" + defaultInt(request.getUrgentRepairCount()),
                            "recentRiskEvents=" + safeList(request.getRecentRiskEvents())),
                    "紧急报修或风险事件可能影响居民安全和物业响应口碑，需要人工确认处理闭环。",
                    "安排值班人员逐条复核风险事件，确认责任人、处理状态和回访结果。"
            ));
        }
        if (defaultInt(request.getComplaintPending()) >= 5) {
            cards.add(card(
                    "COMPLAINT_BACKLOG",
                    "HIGH",
                    "投诉待处理数量偏高，存在升级风险",
                    List.of("complaintTotal=" + defaultInt(request.getComplaintTotal()),
                            "complaintPending=" + defaultInt(request.getComplaintPending()),
                            "residentAppeals=" + safeList(request.getResidentAppeals())),
                    "投诉积压会降低居民感知，若集中在噪音、安全、秩序类问题，容易形成重复投诉。",
                    "将待处理投诉按类型分派责任岗位，优先处理高频诉求并向居民反馈进度。"
            ));
        } else if (defaultInt(request.getComplaintTotal()) > 0) {
            cards.add(card(
                    "COMPLAINT_SIGNAL",
                    "MEDIUM",
                    "本期存在居民诉求，需要关注高频问题",
                    List.of("complaintTotal=" + defaultInt(request.getComplaintTotal()),
                            "residentAppeals=" + safeList(request.getResidentAppeals())),
                    "投诉数量虽未达到高风险阈值，但可以作为服务改进的信号来源。",
                    "复盘居民诉求类型，提炼 1-2 个可在本周改善的服务动作。"
            ));
        }
        if (!isEmpty(request.getTopRepairCategories())) {
            cards.add(card(
                    "REPAIR_HOTSPOT",
                    defaultInt(request.getRepairPending()) >= 10 ? "HIGH" : "MEDIUM",
                    "报修热点已经形成，适合做专项排查",
                    List.of("repairTotal=" + defaultInt(request.getRepairTotal()),
                            "repairPending=" + defaultInt(request.getRepairPending()),
                            "topRepairCategories=" + safeList(request.getTopRepairCategories())),
                    "高频报修类型可能不是单点故障，而是设备、区域或维保策略的问题。",
                    "按报修类型和楼栋位置做二次筛选，安排专项巡检。"
            ));
        }
        if (defaultInt(request.getFeeUnpaidCount()) >= 10) {
            cards.add(card(
                    "FEE_COLLECTION",
                    defaultInt(request.getFeeUnpaidCount()) >= 50 ? "HIGH" : "MEDIUM",
                    "欠费户数需要纳入运营跟进",
                    List.of("feeUnpaidCount=" + defaultInt(request.getFeeUnpaidCount())),
                    "欠费压力会影响物业现金流，也可能暴露居民对服务质量或账单透明度的疑问。",
                    "按欠费周期分层触达，对费用疑问类住户优先安排人工解释。"
            ));
        }
        if (cards.isEmpty()) {
            cards.add(card(
                    "STABLE_OPERATION",
                    "LOW",
                    "本期未发现明显运营风险",
                    List.of("repairTotal=" + defaultInt(request.getRepairTotal()),
                            "complaintTotal=" + defaultInt(request.getComplaintTotal()),
                            "feeUnpaidCount=" + defaultInt(request.getFeeUnpaidCount())),
                    "当前输入指标未触发高风险规则，可以保持日常巡检和服务跟进。",
                    "继续观察下期指标变化，保留报修和投诉明细用于趋势比较。"
            ));
        }
        return cards;
    }

    private List<OperationsActionItem> buildRuleActions(OperationsMetricsSnapshot request) {
        List<OperationsActionItem> actions = new ArrayList<>();
        if (defaultInt(request.getUrgentRepairCount()) > 0 || !isEmpty(request.getRecentRiskEvents())) {
            actions.add(action(
                    "P1",
                    "物业值班负责人",
                    "复核紧急报修和近期风险事件处理闭环",
                    "24小时内",
                    "存在安全或服务升级风险，需要先确认是否已经处理完成。"
            ));
        }
        if (defaultInt(request.getComplaintPending()) >= 5) {
            actions.add(action(
                    "P1",
                    "客服主管",
                    "清理待处理投诉并输出居民反馈进度",
                    "48小时内",
                    "投诉积压会直接影响居民满意度和舆情风险。"
            ));
        }
        if (!isEmpty(request.getTopRepairCategories())) {
            actions.add(action(
                    "P2",
                    "工程维修负责人",
                    "针对高频报修类型安排专项巡检",
                    "本周内",
                    "高频报修可能来自同类设备或区域性隐患。"
            ));
        }
        if (defaultInt(request.getFeeUnpaidCount()) >= 10) {
            actions.add(action(
                    "P2",
                    "收费专员",
                    "对欠费住户做分层提醒和账单解释",
                    "本周内",
                    "欠费户数较多时，单纯催缴不如先识别账单疑问和服务不满。"
            ));
        }
        if (actions.isEmpty()) {
            actions.add(action(
                    "P3",
                    "物业运营负责人",
                    "保留下期趋势对比口径",
                    "下次周报前",
                    "当前风险较低，但稳定的指标口径有利于后续发现异常。"
            ));
        }
        return actions;
    }

    private OperationsInsightCard card(String insightType,
                                       String riskLevel,
                                       String title,
                                       List<String> evidence,
                                       String analysis,
                                       String recommendedAction) {
        OperationsInsightCard card = new OperationsInsightCard();
        card.setInsightType(insightType);
        card.setRiskLevel(riskLevel);
        card.setTitle(title);
        card.setEvidence(evidence);
        card.setAnalysis(analysis);
        card.setRecommendedAction(recommendedAction);
        return card;
    }

    private OperationsActionItem action(String priority,
                                        String ownerRole,
                                        String task,
                                        String deadline,
                                        String reason) {
        OperationsActionItem action = new OperationsActionItem();
        action.setPriority(priority);
        action.setOwnerRole(ownerRole);
        action.setTask(task);
        action.setDeadline(deadline);
        action.setReason(reason);
        return action;
    }

    private String buildFallbackHeadline(OperationsMetricsSnapshot request) {
        String communityName = StringUtils.hasText(request.getCommunityName())
                ? request.getCommunityName()
                : "社区";
        if (defaultInt(request.getUrgentRepairCount()) > 0 || !isEmpty(request.getRecentRiskEvents())) {
            return communityName + "存在需要优先复核的运营风险";
        }
        if (defaultInt(request.getComplaintPending()) >= 5 || defaultInt(request.getFeeUnpaidCount()) >= 10) {
            return communityName + "存在投诉或欠费运营关注点";
        }
        return communityName + "本期运营整体平稳";
    }

    private String buildOperationData(OperationsMetricsSnapshot request) {
        return String.join("\n",
                "communityId: " + request.getCommunityId(),
                "communityName: " + request.getCommunityName(),
                "dateRange: " + request.getStartDate() + " to " + request.getEndDate(),
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

    private String safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.toString();
    }

    private boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}

