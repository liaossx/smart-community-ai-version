package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsInsightCard;
import com.lsx.ai.operations.dto.OperationsMetricsSnapshot;
import com.lsx.ai.operations.dto.OperationsRiskAlert;

import java.util.List;

final class OperationsRiskAssessment {
    private OperationsRiskAssessment() {
    }

    static String overallRiskLevel(OperationsMetricsSnapshot snapshot, List<OperationsInsightCard> cards) {
        if (defaultInt(snapshot.getUrgentRepairCount()) >= 3 || hasCardRiskLevel(cards, "CRITICAL")) {
            return "CRITICAL";
        }
        if (defaultInt(snapshot.getUrgentRepairCount()) > 0
                || defaultInt(snapshot.getComplaintPending()) >= 5
                || hasCardRiskLevel(cards, "HIGH")
                || hasRecentRiskEvents(snapshot)) {
            return "HIGH";
        }
        if (defaultInt(snapshot.getComplaintTotal()) > 0
                || defaultInt(snapshot.getFeeUnpaidCount()) >= 10
                || hasCardRiskLevel(cards, "MEDIUM")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    static boolean needsManualReview(OperationsMetricsSnapshot snapshot, String riskLevel) {
        return "HIGH".equals(riskLevel)
                || "CRITICAL".equals(riskLevel)
                || defaultInt(snapshot.getUrgentRepairCount()) > 0
                || defaultInt(snapshot.getComplaintPending()) >= 5
                || hasRecentRiskEvents(snapshot);
    }

    static boolean hasOperationalSignals(OperationsMetricsSnapshot snapshot) {
        return defaultInt(snapshot.getRepairTotal()) > 0
                || defaultInt(snapshot.getComplaintTotal()) > 0
                || defaultInt(snapshot.getVisitorTotal()) > 0
                || defaultInt(snapshot.getFeeUnpaidCount()) > 0
                || defaultInt(snapshot.getNoticePublishedCount()) > 0
                || !isEmpty(snapshot.getTopRepairCategories())
                || !isEmpty(snapshot.getResidentAppeals())
                || hasRecentRiskEvents(snapshot);
    }

    static boolean hasHighRiskSignals(OperationsMetricsSnapshot snapshot) {
        return defaultInt(snapshot.getUrgentRepairCount()) > 0
                || defaultInt(snapshot.getComplaintPending()) >= 5
                || hasRecentRiskEvents(snapshot);
    }

    static boolean hasHighRiskAlerts(List<OperationsRiskAlert> risks) {
        if (risks == null) {
            return false;
        }
        return risks.stream()
                .map(OperationsRiskAlert::getRiskLevel)
                .anyMatch(level -> "HIGH".equals(level) || "CRITICAL".equals(level));
    }

    private static boolean hasCardRiskLevel(List<OperationsInsightCard> cards, String riskLevel) {
        if (cards == null) {
            return false;
        }
        return cards.stream().anyMatch(card -> riskLevel.equals(card.getRiskLevel()));
    }

    private static boolean hasRecentRiskEvents(OperationsMetricsSnapshot snapshot) {
        return !isEmpty(snapshot.getRecentRiskEvents());
    }

    private static boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private static int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
