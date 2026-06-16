package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsMetricsSnapshot;
import com.lsx.ai.operations.dto.OperationsReportResponse;

public interface OperationsReportAssistant {
    OperationsReportResponse generateWeeklyReport(OperationsMetricsSnapshot request);
}

