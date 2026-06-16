package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsInsightsResponse;
import com.lsx.ai.operations.dto.OperationsMetricsSnapshot;

public interface OperationsInsightsAssistant {
    OperationsInsightsResponse generateInsights(OperationsMetricsSnapshot request);
}

