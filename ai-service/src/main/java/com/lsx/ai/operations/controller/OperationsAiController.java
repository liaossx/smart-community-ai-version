package com.lsx.ai.operations.controller;

import com.lsx.ai.operations.dto.OperationsInsightsFromDbResponse;
import com.lsx.ai.operations.dto.OperationsInsightsResponse;
import com.lsx.ai.operations.dto.OperationsActionTaskCreateRequest;
import com.lsx.ai.operations.dto.OperationsActionTaskCreateResponse;
import com.lsx.ai.operations.dto.OperationsActionTaskItem;
import com.lsx.ai.operations.dto.OperationsActionTaskPageResponse;
import com.lsx.ai.operations.dto.OperationsActionTaskUpdateRequest;
import com.lsx.ai.operations.dto.OperationsReportFromDbResponse;
import com.lsx.ai.operations.dto.OperationsMetricsSnapshot;
import com.lsx.ai.operations.dto.OperationsReportRequest;
import com.lsx.ai.operations.dto.OperationsReportResponse;
import com.lsx.ai.operations.service.OperationsActionTaskService;
import com.lsx.ai.operations.service.OperationsInsightsAssistant;
import com.lsx.ai.operations.service.OperationsMetricsAggregationService;
import com.lsx.ai.operations.service.OperationsReportAssistant;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ai/operations")
public class OperationsAiController {
    private final OperationsReportAssistant assistant;
    private final OperationsInsightsAssistant insightsAssistant;
    private final OperationsMetricsAggregationService metricsAggregationService;
    private final OperationsActionTaskService actionTaskService;

    public OperationsAiController(OperationsReportAssistant assistant,
                                  OperationsInsightsAssistant insightsAssistant,
                                  OperationsMetricsAggregationService metricsAggregationService,
                                  OperationsActionTaskService actionTaskService) {
        this.assistant = assistant;
        this.insightsAssistant = insightsAssistant;
        this.metricsAggregationService = metricsAggregationService;
        this.actionTaskService = actionTaskService;
    }

    @PostMapping("/weekly-report")
    public OperationsReportResponse generateWeeklyReport(@Valid @RequestBody OperationsReportRequest request) {
        // 手动聚合模式：外部系统已经准备好运营指标，ai-service 只负责调用模型生成报告。
        return assistant.generateWeeklyReport(request);
    }

    @GetMapping("/weekly-report/from-db")
    public OperationsReportFromDbResponse generateWeeklyReportFromDb(
            @RequestParam(value = "communityId", required = false) Long communityId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // 自动聚合模式：先从业务表汇总 sourceData，再把 sourceData 交给运营 AI 助手生成周报。
        OperationsMetricsSnapshot sourceData =
                metricsAggregationService.aggregateWeeklyReportData(communityId, startDate, endDate);
        OperationsReportResponse report = assistant.generateWeeklyReport(sourceData);
        return new OperationsReportFromDbResponse("MYSQL", sourceData, report);
    }

    @PostMapping("/insights")
    public OperationsInsightsResponse generateInsights(@Valid @RequestBody OperationsReportRequest request) {
        return insightsAssistant.generateInsights(request);
    }

    @GetMapping("/insights/from-db")
    public OperationsInsightsFromDbResponse generateInsightsFromDb(
            @RequestParam(value = "communityId", required = false) Long communityId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        OperationsMetricsSnapshot sourceData =
                metricsAggregationService.aggregateWeeklyReportData(communityId, startDate, endDate);
        OperationsInsightsResponse insights = insightsAssistant.generateInsights(sourceData);
        return new OperationsInsightsFromDbResponse("MYSQL", sourceData, insights);
    }

    @PostMapping("/action-tasks/from-insights")
    public OperationsActionTaskCreateResponse createActionTasksFromInsights(
            @Valid @RequestBody OperationsActionTaskCreateRequest request) {
        return actionTaskService.createFromInsights(request);
    }

    @GetMapping("/action-tasks")
    public OperationsActionTaskPageResponse listActionTasks(
            @RequestParam(value = "communityId", required = false) Long communityId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "taskBatchNo", required = false) String taskBatchNo,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return actionTaskService.list(communityId, status, taskBatchNo, pageNum, pageSize);
    }

    @GetMapping("/action-tasks/{id}")
    public OperationsActionTaskItem getActionTask(@PathVariable("id") Long id) {
        return actionTaskService.getRequired(id);
    }

    @PutMapping("/action-tasks/{id}/status")
    public OperationsActionTaskItem updateActionTaskStatus(@PathVariable("id") Long id,
                                                           @Valid @RequestBody OperationsActionTaskUpdateRequest request) {
        return actionTaskService.updateStatus(id, request);
    }
}


