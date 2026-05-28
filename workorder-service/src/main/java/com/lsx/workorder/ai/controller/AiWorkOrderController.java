package com.lsx.workorder.ai.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.workorder.ai.entity.AiWorkOrderAnalysis;
import com.lsx.workorder.ai.service.AiWorkOrderAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workorder/ai")
@Tag(
        name = "AI Work Order Assistant",
        description = "Phase-one AI assistant APIs for repair report analysis, latest snapshot reads, and history reads"
)
public class AiWorkOrderController {

    private final AiWorkOrderAnalysisService aiWorkOrderAnalysisService;

    public AiWorkOrderController(AiWorkOrderAnalysisService aiWorkOrderAnalysisService) {
        this.aiWorkOrderAnalysisService = aiWorkOrderAnalysisService;
    }

    @PostMapping("/repair/{repairId}/analyze")
    @Operation(
            summary = "Create AI analysis snapshot",
            description = "Analyze an existing repair report with the rule-first provider, persist a new AI analysis snapshot, and mark it as latest."
    )
    public Result<AiWorkOrderAnalysis> analyzeRepair(
            @Parameter(description = "Repair report id", required = true)
            @PathVariable Long repairId) {
        try {
            return Result.success(aiWorkOrderAnalysisService.analyzeRepair(repairId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/repair/{repairId}/latest")
    @Operation(
            summary = "Get latest AI analysis snapshot",
            description = "Return the latest AI analysis snapshot for an existing repair report without creating a new snapshot."
    )
    public Result<AiWorkOrderAnalysis> getLatestAnalysis(
            @Parameter(description = "Repair report id", required = true)
            @PathVariable Long repairId) {
        try {
            return Result.success(aiWorkOrderAnalysisService.getLatestAnalysis(repairId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/repair/{repairId}/history")
    @Operation(
            summary = "Get AI analysis snapshot history",
            description = "Return all AI analysis snapshots for an existing repair report, ordered newest first for audit usage."
    )
    public Result<List<AiWorkOrderAnalysis>> getAnalysisHistory(
            @Parameter(description = "Repair report id", required = true)
            @PathVariable Long repairId) {
        try {
            return Result.success(aiWorkOrderAnalysisService.getAnalysisHistory(repairId));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
