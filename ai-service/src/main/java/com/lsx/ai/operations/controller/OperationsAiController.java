package com.lsx.ai.operations.controller;

import com.lsx.ai.operations.dto.OperationsReportFromDbResponse;
import com.lsx.ai.operations.dto.OperationsReportRequest;
import com.lsx.ai.operations.dto.OperationsReportResponse;
import com.lsx.ai.operations.service.OperationsMetricsAggregationService;
import com.lsx.ai.operations.service.OperationsReportAssistant;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ai/operations")
public class OperationsAiController {
    private final OperationsReportAssistant assistant;
    private final OperationsMetricsAggregationService metricsAggregationService;

    public OperationsAiController(OperationsReportAssistant assistant,
                                  OperationsMetricsAggregationService metricsAggregationService) {
        this.assistant = assistant;
        this.metricsAggregationService = metricsAggregationService;
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
        OperationsReportRequest sourceData =
                metricsAggregationService.aggregateWeeklyReportData(communityId, startDate, endDate);
        OperationsReportResponse report = assistant.generateWeeklyReport(sourceData);
        return new OperationsReportFromDbResponse("MYSQL", sourceData, report);
    }
}
