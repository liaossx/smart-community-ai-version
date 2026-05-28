package com.lsx.ai.workorder.controller;

import com.lsx.ai.workorder.dto.WorkOrderAnalyzeRequest;
import com.lsx.ai.workorder.dto.WorkOrderAnalyzeResponse;
import com.lsx.ai.workorder.service.WorkOrderAiAnalyzer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/workorder")
public class WorkOrderAiController {
    private final WorkOrderAiAnalyzer analyzer;

    public WorkOrderAiController(WorkOrderAiAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @PostMapping("/analyze")
    public WorkOrderAnalyzeResponse analyze(@Valid @RequestBody WorkOrderAnalyzeRequest request) {
        return analyzer.analyze(request);
    }
}
