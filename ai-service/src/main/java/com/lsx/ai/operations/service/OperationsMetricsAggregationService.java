package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsReportRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OperationsMetricsAggregationService {
    /*
     * 运营助手里的“规则优先”部分。
     *
     * 这些关键词不交给模型判断，而是在 SQL 聚合阶段先识别出来，
     * 用来计算 urgentRepairCount 和 recentRiskEvents。
     */
    private static final List<String> HIGH_RISK_REPAIR_KEYWORDS = List.of(
            "漏电", "燃气", "电梯困人", "起火", "冒烟", "消防", "爆管", "大面积积水", "积水", "无法进出"
    );
    private static final List<String> HIGH_RISK_COMPLAINT_KEYWORDS = List.of(
            "高空抛物", "消防", "燃气", "漏电", "电动车", "安全", "冲突", "打架", "堵塞", "噪音"
    );

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public OperationsMetricsAggregationService(
            @Value("${smart-community.ai.operations.jdbc.url}") String jdbcUrl,
            @Value("${smart-community.ai.operations.jdbc.username}") String username,
            @Value("${smart-community.ai.operations.jdbc.password}") String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public OperationsReportRequest aggregateWeeklyReportData(Long communityId,
                                                             LocalDate startDate,
                                                             LocalDate endDate) {
        validateDateRange(startDate, endDate);

        // endDate 是用户选择的自然日，SQL 用右开区间，避免漏掉当天 23:59:59 的数据。
        Timestamp startTime = Timestamp.valueOf(startDate.atStartOfDay());
        Timestamp endExclusive = Timestamp.valueOf(endDate.plusDays(1).atStartOfDay());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            // sourceData 是“给大模型的事实材料”。模型只能总结这些数据，不直接查库。
            OperationsReportRequest request = new OperationsReportRequest();
            request.setCommunityId(communityId);
            request.setCommunityName(resolveCommunityName(connection, communityId));
            request.setStartDate(startDate.toString());
            request.setEndDate(endDate.toString());

            request.setRepairTotal(countRepairs(connection, communityId, startTime, endExclusive));
            request.setRepairPending(countPendingRepairs(connection, communityId, startTime, endExclusive));
            request.setRepairCompleted(countCompletedRepairs(connection, communityId, startTime, endExclusive));
            request.setUrgentRepairCount(countUrgentRepairs(connection, communityId, startTime, endExclusive));
            request.setComplaintTotal(countComplaints(connection, communityId, startTime, endExclusive));
            request.setComplaintPending(countPendingComplaints(connection, communityId, startTime, endExclusive));
            request.setVisitorTotal(countVisitors(connection, communityId, startTime, endExclusive));
            request.setFeeUnpaidCount(countUnpaidFees(connection, communityId));
            request.setNoticePublishedCount(countPublishedNotices(connection, communityId, startTime, endExclusive));
            request.setTopRepairCategories(queryTopRepairCategories(connection, communityId, startTime, endExclusive));
            request.setResidentAppeals(queryResidentAppeals(connection, communityId, startTime, endExclusive));
            request.setRecentRiskEvents(queryRecentRiskEvents(connection, communityId, startTime, endExclusive, request));
            return request;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "load operations metrics failed: " + ex.getMessage(), ex);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be greater than or equal to startDate");
        }
    }

    private String resolveCommunityName(Connection connection, Long communityId) throws SQLException {
        if (communityId == null) {
            return "全部社区";
        }
        String sql = "SELECT name FROM sys_community WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, communityId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && StringUtils.hasText(resultSet.getString("name"))) {
                    return resultSet.getString("name");
                }
            }
        }
        return "社区" + communityId;
    }

    private int countRepairs(Connection connection, Long communityId, Timestamp startTime, Timestamp endExclusive)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM biz_repair r WHERE r.create_time >= ? AND r.create_time < ?"
                + communityFilter("r", communityId);
        return queryCount(connection, sql, statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
        });
    }

    private int countPendingRepairs(Connection connection, Long communityId, Timestamp startTime, Timestamp endExclusive)
            throws SQLException {
        String sql = String.join(" ",
                "SELECT COUNT(*) FROM biz_repair r",
                "WHERE r.create_time >= ? AND r.create_time < ?",
                "AND LOWER(COALESCE(r.status, '')) IN ('pending', 'processing')")
                + communityFilter("r", communityId);
        return queryCount(connection, sql, statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
        });
    }

    private int countCompletedRepairs(Connection connection, Long communityId, Timestamp startTime, Timestamp endExclusive)
            throws SQLException {
        String sql = String.join(" ",
                "SELECT COUNT(*) FROM biz_repair r",
                "WHERE r.create_time >= ? AND r.create_time < ?",
                "AND LOWER(COALESCE(r.status, '')) = 'completed'")
                + communityFilter("r", communityId);
        return queryCount(connection, sql, statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
        });
    }

    private int countUrgentRepairs(Connection connection, Long communityId, Timestamp startTime, Timestamp endExclusive)
            throws SQLException {
        // 紧急报修 = 工单优先级较高，或报修描述命中高风险关键词。
        String sql = String.join(" ",
                "SELECT COUNT(DISTINCT r.id)",
                "FROM biz_repair r",
                "LEFT JOIN biz_work_order wo ON wo.repair_id = r.id",
                "WHERE r.create_time >= ? AND r.create_time < ?")
                + repairCommunityFilter(communityId)
                + " AND (COALESCE(wo.priority, 1) >= 2 OR " + repairKeywordCondition() + ")";
        return queryCount(connection, sql, statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            index = bindRepairCommunity(statement, index, communityId);
            bindLikeKeywords(statement, index, HIGH_RISK_REPAIR_KEYWORDS, 2);
        });
    }

    private int countComplaints(Connection connection, Long communityId, Timestamp startTime, Timestamp endExclusive)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM sys_complaint c WHERE c.create_time >= ? AND c.create_time < ?"
                + communityFilter("c", communityId);
        return queryCount(connection, sql, statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
        });
    }

    private int countPendingComplaints(Connection connection, Long communityId, Timestamp startTime, Timestamp endExclusive)
            throws SQLException {
        String sql = String.join(" ",
                "SELECT COUNT(*) FROM sys_complaint c",
                "WHERE c.create_time >= ? AND c.create_time < ?",
                "AND UPPER(COALESCE(c.status, '')) IN ('PENDING', 'PROCESSING')")
                + communityFilter("c", communityId);
        return queryCount(connection, sql, statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
        });
    }

    private int countVisitors(Connection connection, Long communityId, Timestamp startTime, Timestamp endExclusive)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM sys_visitor v WHERE v.visit_time >= ? AND v.visit_time < ?"
                + communityFilter("v", communityId);
        return queryCount(connection, sql, statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
        });
    }

    private int countUnpaidFees(Connection connection, Long communityId) throws SQLException {
        // 欠费是当前存量风险，不按周过滤；周报里关注的是“目前还有多少未缴”。
        String sql = String.join(" ",
                "SELECT COUNT(*) FROM sys_fee f",
                "WHERE UPPER(COALESCE(f.status, '')) IN ('UNPAID', 'PAYING', 'OVERDUE')")
                + communityFilter("f", communityId);
        return queryCount(connection, sql, statement -> bindCommunity(statement, 1, communityId));
    }

    private int countPublishedNotices(Connection connection, Long communityId, Timestamp startTime, Timestamp endExclusive)
            throws SQLException {
        String sql = String.join(" ",
                "SELECT COUNT(*) FROM sys_notice n",
                "WHERE n.deleted = 0",
                "AND n.publish_status = 'PUBLISHED'",
                "AND COALESCE(n.publish_time, n.create_time) >= ?",
                "AND COALESCE(n.publish_time, n.create_time) < ?")
                + communityFilter("n", communityId);
        return queryCount(connection, sql, statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
        });
    }

    private List<String> queryTopRepairCategories(Connection connection, Long communityId,
                                                  Timestamp startTime, Timestamp endExclusive) throws SQLException {
        String sql = String.join(" ",
                "SELECT COALESCE(NULLIF(TRIM(r.fault_type), ''), '未分类') AS category, COUNT(*) AS total",
                "FROM biz_repair r",
                "WHERE r.create_time >= ? AND r.create_time < ?")
                + communityFilter("r", communityId)
                + " GROUP BY category ORDER BY total DESC, category ASC LIMIT 5";
        return queryNameCounts(connection, sql, "category", "单", statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
        });
    }

    private List<String> queryResidentAppeals(Connection connection, Long communityId,
                                              Timestamp startTime, Timestamp endExclusive) throws SQLException {
        String sql = String.join(" ",
                "SELECT COALESCE(NULLIF(TRIM(c.type), ''), '未分类') AS appeal_type, COUNT(*) AS total",
                "FROM sys_complaint c",
                "WHERE c.create_time >= ? AND c.create_time < ?")
                + communityFilter("c", communityId)
                + " GROUP BY appeal_type ORDER BY total DESC, appeal_type ASC LIMIT 5";
        List<String> appeals = queryNameCounts(connection, sql, "appeal_type", "件", statement -> {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
        });
        if (!appeals.isEmpty()) {
            return appeals;
        }
        return queryRecentComplaintContents(connection, communityId, startTime, endExclusive);
    }

    private List<String> queryRecentComplaintContents(Connection connection, Long communityId,
                                                      Timestamp startTime, Timestamp endExclusive) throws SQLException {
        String sql = String.join(" ",
                "SELECT c.content",
                "FROM sys_complaint c",
                "WHERE c.create_time >= ? AND c.create_time < ?",
                "AND c.content IS NOT NULL AND c.content <> ''")
                + communityFilter("c", communityId)
                + " ORDER BY c.create_time DESC, c.id DESC LIMIT 5";
        List<String> contents = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            bindCommunity(statement, index, communityId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    contents.add(truncate(resultSet.getString("content"), 40));
                }
            }
        }
        return contents;
    }

    private List<String> queryRecentRiskEvents(Connection connection,
                                               Long communityId,
                                               Timestamp startTime,
                                               Timestamp endExclusive,
                                               OperationsReportRequest request) throws SQLException {
        // recentRiskEvents 是喂给模型的“重点风险事实”，让模型能生成更像运营周报的风险提醒。
        List<String> events = new ArrayList<>();
        events.addAll(queryRiskRepairEvents(connection, communityId, startTime, endExclusive));
        if (events.size() < 5) {
            events.addAll(queryRiskComplaintEvents(connection, communityId, startTime, endExclusive, 5 - events.size()));
        }
        if (defaultInt(request.getComplaintPending()) >= 5) {
            events.add("待处理投诉累计" + request.getComplaintPending() + "件，建议安排人工复核。");
        }
        if (defaultInt(request.getFeeUnpaidCount()) >= 20) {
            events.add("当前未缴费账单" + request.getFeeUnpaidCount() + "笔，建议关注催缴节奏。");
        }
        return events.size() > 5 ? events.subList(0, 5) : events;
    }

    private List<String> queryRiskRepairEvents(Connection connection, Long communityId,
                                               Timestamp startTime, Timestamp endExclusive) throws SQLException {
        String sql = String.join(" ",
                "SELECT r.id, r.fault_type, r.fault_desc, COALESCE(wo.priority, 1) AS priority",
                "FROM biz_repair r",
                "LEFT JOIN biz_work_order wo ON wo.repair_id = r.id",
                "WHERE r.create_time >= ? AND r.create_time < ?")
                + repairCommunityFilter(communityId)
                + " AND (COALESCE(wo.priority, 1) >= 2 OR " + repairKeywordCondition() + ")"
                + " ORDER BY priority DESC, r.create_time DESC, r.id DESC LIMIT 5";
        List<String> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            index = bindRepairCommunity(statement, index, communityId);
            bindLikeKeywords(statement, index, HIGH_RISK_REPAIR_KEYWORDS, 2);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add("报修#" + resultSet.getLong("id")
                            + "：" + valueOrDefault(resultSet.getString("fault_type"), "未分类")
                            + "，" + truncate(resultSet.getString("fault_desc"), 40)
                            + "（优先级" + resultSet.getInt("priority") + "）");
                }
            }
        }
        return events;
    }

    private List<String> queryRiskComplaintEvents(Connection connection, Long communityId,
                                                  Timestamp startTime, Timestamp endExclusive,
                                                  int limit) throws SQLException {
        if (limit <= 0) {
            return List.of();
        }
        String sql = String.join(" ",
                "SELECT c.id, c.type, c.content",
                "FROM sys_complaint c",
                "WHERE c.create_time >= ? AND c.create_time < ?",
                "AND UPPER(COALESCE(c.status, '')) IN ('PENDING', 'PROCESSING')")
                + communityFilter("c", communityId)
                + " AND (" + complaintKeywordCondition() + ")"
                + " ORDER BY c.create_time DESC, c.id DESC LIMIT " + limit;
        List<String> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindDateRange(statement, 1, startTime, endExclusive);
            index = bindCommunity(statement, index, communityId);
            bindLikeKeywords(statement, index, HIGH_RISK_COMPLAINT_KEYWORDS, 2);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add("投诉#" + resultSet.getLong("id")
                            + "：" + valueOrDefault(resultSet.getString("type"), "未分类")
                            + "，" + truncate(resultSet.getString("content"), 40));
                }
            }
        }
        return events;
    }

    private int queryCount(Connection connection, String sql, StatementBinder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    private List<String> queryNameCounts(Connection connection, String sql, String nameColumn, String unit,
                                         StatementBinder binder) throws SQLException {
        List<String> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString(nameColumn) + "：" + resultSet.getInt("total") + unit);
                }
            }
        }
        return values;
    }

    private String communityFilter(String alias, Long communityId) {
        // communityId 为空表示统计全部社区；不为空则只统计指定社区。
        return communityId == null ? "" : " AND " + alias + ".community_id = ?";
    }

    private String repairCommunityFilter(Long communityId) {
        return communityId == null ? "" : " AND (r.community_id = ? OR wo.community_id = ?)";
    }

    private int bindDateRange(PreparedStatement statement, int startIndex,
                              Timestamp startTime, Timestamp endExclusive) throws SQLException {
        statement.setTimestamp(startIndex, startTime);
        statement.setTimestamp(startIndex + 1, endExclusive);
        return startIndex + 2;
    }

    private int bindCommunity(PreparedStatement statement, int startIndex, Long communityId) throws SQLException {
        if (communityId == null) {
            return startIndex;
        }
        statement.setLong(startIndex, communityId);
        return startIndex + 1;
    }

    private int bindRepairCommunity(PreparedStatement statement, int startIndex, Long communityId) throws SQLException {
        if (communityId == null) {
            return startIndex;
        }
        statement.setLong(startIndex, communityId);
        statement.setLong(startIndex + 1, communityId);
        return startIndex + 2;
    }

    private void bindLikeKeywords(PreparedStatement statement, int startIndex,
                                  List<String> keywords, int columnCount) throws SQLException {
        int index = startIndex;
        for (String keyword : keywords) {
            for (int i = 0; i < columnCount; i++) {
                statement.setString(index++, "%" + keyword + "%");
            }
        }
    }

    private String repairKeywordCondition() {
        return keywordCondition("r.fault_type", "r.fault_desc", HIGH_RISK_REPAIR_KEYWORDS.size());
    }

    private String complaintKeywordCondition() {
        return keywordCondition("c.type", "c.content", HIGH_RISK_COMPLAINT_KEYWORDS.size());
    }

    private String keywordCondition(String firstColumn, String secondColumn, int keywordCount) {
        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < keywordCount; i++) {
            conditions.add("COALESCE(" + firstColumn + ", '') LIKE ? OR COALESCE(" + secondColumn + ", '') LIKE ?");
        }
        return String.join(" OR ", conditions);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "无描述";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "...";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
