package com.lsx.ai.operations.dto;

/**
 * API compatibility type for manual weekly-report requests.
 *
 * Internally, both weekly reports and insights work from the neutral
 * OperationsMetricsSnapshot so their data contract stays shared.
 */
public class OperationsReportRequest extends OperationsMetricsSnapshot {
}
