package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsReportRequest;
import com.lsx.ai.operations.dto.OperationsReportResponse;

public interface OperationsReportAssistant {
    OperationsReportResponse generateWeeklyReport(OperationsReportRequest request);
}
