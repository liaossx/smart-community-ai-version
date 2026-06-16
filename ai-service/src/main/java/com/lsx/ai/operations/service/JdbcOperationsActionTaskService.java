package com.lsx.ai.operations.service;

import com.lsx.ai.operations.dto.OperationsActionItem;
import com.lsx.ai.operations.dto.OperationsActionTaskCreateRequest;
import com.lsx.ai.operations.dto.OperationsActionTaskCreateResponse;
import com.lsx.ai.operations.dto.OperationsActionTaskItem;
import com.lsx.ai.operations.dto.OperationsActionTaskPageResponse;
import com.lsx.ai.operations.dto.OperationsActionTaskUpdateRequest;
import com.lsx.ai.operations.dto.OperationsInsightsResponse;
import com.lsx.ai.operations.dto.OperationsMetricsSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class JdbcOperationsActionTaskService implements OperationsActionTaskService {
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final String INSERT_TASK = String.join("\n",
            "INSERT INTO ai_operations_action_task",
            "(task_batch_no, community_id, community_name, source, source_version, source_window_start, source_window_end,",
            " overall_risk_level, priority, owner_role, task_title, task_reason, deadline_text, status,",
            " feedback_result, feedback_by, feedback_time, closed_loop)",
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    );

    private static final String FIND_TASK_BY_ID = String.join("\n",
            "SELECT id, task_batch_no, community_id, community_name, source, source_version, source_window_start,",
            "       source_window_end, overall_risk_level, priority, owner_role, task_title, task_reason, deadline_text,",
            "       status, feedback_result, feedback_by, feedback_time, closed_loop, create_time, update_time",
            "FROM ai_operations_action_task",
            "WHERE id = ?"
    );

    private static final String UPDATE_TASK = String.join("\n",
            "UPDATE ai_operations_action_task",
            "SET status = ?, feedback_result = ?, feedback_by = ?, feedback_time = ?, closed_loop = ?, update_time = CURRENT_TIMESTAMP",
            "WHERE id = ?"
    );

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final OperationsMetricsAggregationService metricsAggregationService;
    private final OperationsInsightsAssistant insightsAssistant;

    public JdbcOperationsActionTaskService(
            @Value("${smart-community.ai.operations.jdbc.url}") String jdbcUrl,
            @Value("${smart-community.ai.operations.jdbc.username}") String username,
            @Value("${smart-community.ai.operations.jdbc.password}") String password,
            OperationsMetricsAggregationService metricsAggregationService,
            OperationsInsightsAssistant insightsAssistant) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.metricsAggregationService = metricsAggregationService;
        this.insightsAssistant = insightsAssistant;
    }

    @Override
    public OperationsActionTaskCreateResponse createFromInsights(OperationsActionTaskCreateRequest request) {
        LocalDate startDate = parseDate(request.getStartDate(), "startDate");
        LocalDate endDate = parseDate(request.getEndDate(), "endDate");
        OperationsMetricsSnapshot sourceData =
                metricsAggregationService.aggregateWeeklyReportData(request.getCommunityId(), startDate, endDate);
        OperationsInsightsResponse insights = insightsAssistant.generateInsights(sourceData);

        List<OperationsActionItem> actionItems = insights.getActionItems() == null
                ? List.of()
                : insights.getActionItems();
        if (actionItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no actionItems generated from insights");
        }

        String batchNo = "ops-task-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        List<OperationsActionTaskItem> createdTasks = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            connection.setAutoCommit(false);
            try {
                for (OperationsActionItem action : actionItems) {
                    OperationsActionTaskItem item = insertTask(connection, batchNo, sourceData, insights, action);
                    createdTasks.add(item);
                }
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "create operations action tasks failed: " + ex.getMessage(), ex);
        }

        OperationsActionTaskCreateResponse response = new OperationsActionTaskCreateResponse();
        response.setTaskBatchNo(batchNo);
        response.setSource("AI_INSIGHTS");
        response.setSourceData(sourceData);
        response.setInsights(insights);
        response.setCreatedCount(createdTasks.size());
        response.setTasks(createdTasks);
        return response;
    }

    @Override
    public OperationsActionTaskPageResponse list(Long communityId, String status, String taskBatchNo,
                                                 Integer pageNum, Integer pageSize) {
        int safePageNum = safePageNum(pageNum);
        int safePageSize = safePageSize(pageSize);
        List<Object> params = new ArrayList<>();
        String whereSql = buildWhereSql(communityId, status, taskBatchNo, params);
        String countSql = "SELECT COUNT(*) FROM ai_operations_action_task WHERE " + whereSql;
        String querySql = String.join("\n",
                "SELECT id, task_batch_no, community_id, community_name, source, source_version, source_window_start,",
                "       source_window_end, overall_risk_level, priority, owner_role, task_title, task_reason, deadline_text,",
                "       status, feedback_result, feedback_by, feedback_time, closed_loop, create_time, update_time",
                "FROM ai_operations_action_task",
                "WHERE " + whereSql,
                "ORDER BY create_time DESC, id DESC",
                "LIMIT ? OFFSET ?"
        );

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            long total = count(connection, countSql, params);
            List<OperationsActionTaskItem> records = queryPage(connection, querySql, params,
                    safePageSize, (safePageNum - 1) * safePageSize);
            OperationsActionTaskPageResponse response = new OperationsActionTaskPageResponse();
            response.setTotal(total);
            response.setPageNum(safePageNum);
            response.setPageSize(safePageSize);
            response.setRecords(records);
            return response;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "list operations action tasks failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public OperationsActionTaskItem getRequired(Long id) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(FIND_TASK_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return toItem(resultSet);
                }
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "load operations action task failed: " + ex.getMessage(), ex);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "operations action task not found: " + id);
    }

    @Override
    public OperationsActionTaskItem updateStatus(Long id, OperationsActionTaskUpdateRequest request) {
        OperationsActionTaskItem existing = getRequired(id);
        String normalizedStatus = normalizeStatus(request.getStatus());
        boolean closedLoop = "DONE".equals(normalizedStatus) || "CANCELLED".equals(normalizedStatus);
        LocalDateTime feedbackTime = StringUtils.hasText(request.getFeedbackResult()) || StringUtils.hasText(request.getFeedbackBy())
                ? LocalDateTime.now()
                : existing.getFeedbackTime();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(UPDATE_TASK)) {
            statement.setString(1, normalizedStatus);
            statement.setString(2, truncate(request.getFeedbackResult(), 1000));
            statement.setString(3, truncate(request.getFeedbackBy(), 100));
            statement.setTimestamp(4, toTimestamp(feedbackTime));
            statement.setBoolean(5, closedLoop);
            statement.setLong(6, id);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "operations action task not found: " + id);
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "update operations action task failed: " + ex.getMessage(), ex);
        }
        return getRequired(id);
    }

    private OperationsActionTaskItem insertTask(Connection connection,
                                                String batchNo,
                                                OperationsMetricsSnapshot sourceData,
                                                OperationsInsightsResponse insights,
                                                OperationsActionItem action) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_TASK, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, batchNo);
            statement.setObject(2, sourceData.getCommunityId());
            statement.setString(3, truncate(sourceData.getCommunityName(), 100));
            statement.setString(4, "AI_INSIGHTS");
            statement.setString(5, truncate(insights.getProviderVersion(), 64));
            statement.setObject(6, parseDate(sourceData.getStartDate(), "startDate"));
            statement.setObject(7, parseDate(sourceData.getEndDate(), "endDate"));
            statement.setString(8, truncate(insights.getOverallRiskLevel(), 20));
            statement.setString(9, normalizePriority(action.getPriority()));
            statement.setString(10, truncate(action.getOwnerRole(), 100));
            statement.setString(11, truncate(action.getTask(), 255));
            statement.setString(12, truncate(action.getReason(), 1000));
            statement.setString(13, truncate(action.getDeadline(), 100));
            statement.setString(14, "TODO");
            statement.setString(15, null);
            statement.setString(16, null);
            statement.setTimestamp(17, null);
            statement.setBoolean(18, false);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("failed to resolve created task id");
                }
                return findById(connection, keys.getLong(1));
            }
        }
    }

    private String buildWhereSql(Long communityId, String status, String taskBatchNo, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("1 = 1");
        if (communityId != null) {
            conditions.add("community_id = ?");
            params.add(communityId);
        }
        if (StringUtils.hasText(status)) {
            conditions.add("status = ?");
            params.add(normalizeStatus(status));
        }
        if (StringUtils.hasText(taskBatchNo)) {
            conditions.add("task_batch_no = ?");
            params.add(taskBatchNo.trim());
        }
        return String.join(" AND ", conditions);
    }

    private long count(Connection connection, String countSql, List<Object> params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(countSql)) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        return 0L;
    }

    private List<OperationsActionTaskItem> queryPage(Connection connection, String querySql, List<Object> params,
                                                     int pageSize, int offset) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(querySql)) {
            bindParams(statement, params);
            statement.setInt(params.size() + 1, pageSize);
            statement.setInt(params.size() + 2, offset);
            List<OperationsActionTaskItem> records = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(toItem(resultSet));
                }
            }
            return records;
        }
    }

    private void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }

    private OperationsActionTaskItem toItem(ResultSet resultSet) throws SQLException {
        OperationsActionTaskItem item = new OperationsActionTaskItem();
        item.setId(resultSet.getLong("id"));
        item.setTaskBatchNo(resultSet.getString("task_batch_no"));
        item.setCommunityId(resultSet.getObject("community_id", Long.class));
        item.setCommunityName(resultSet.getString("community_name"));
        item.setSource(resultSet.getString("source"));
        item.setSourceVersion(resultSet.getString("source_version"));
        item.setSourceWindowStart(toLocalDate(resultSet.getDate("source_window_start")));
        item.setSourceWindowEnd(toLocalDate(resultSet.getDate("source_window_end")));
        item.setOverallRiskLevel(resultSet.getString("overall_risk_level"));
        item.setPriority(resultSet.getString("priority"));
        item.setOwnerRole(resultSet.getString("owner_role"));
        item.setTaskTitle(resultSet.getString("task_title"));
        item.setTaskReason(resultSet.getString("task_reason"));
        item.setDeadlineText(resultSet.getString("deadline_text"));
        item.setStatus(resultSet.getString("status"));
        item.setFeedbackResult(resultSet.getString("feedback_result"));
        item.setFeedbackBy(resultSet.getString("feedback_by"));
        item.setFeedbackTime(toLocalDateTime(resultSet.getTimestamp("feedback_time")));
        item.setClosedLoop(resultSet.getBoolean("closed_loop"));
        item.setCreateTime(toLocalDateTime(resultSet.getTimestamp("create_time")));
        item.setUpdateTime(toLocalDateTime(resultSet.getTimestamp("update_time")));
        return item;
    }

    private OperationsActionTaskItem findById(Connection connection, Long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_TASK_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return toItem(resultSet);
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "operations action task not found: " + id);
    }

    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private int safePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeStatus(String status) {
        String upper = status == null ? "TODO" : status.trim().toUpperCase();
        if ("TODO".equals(upper) || "IN_PROGRESS".equals(upper) || "BLOCKED".equals(upper)
                || "DONE".equals(upper) || "CANCELLED".equals(upper)) {
            return upper;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid task status: " + status);
    }

    private String normalizePriority(String priority) {
        String upper = priority == null ? "P2" : priority.trim().toUpperCase();
        if ("P0".equals(upper) || "P1".equals(upper) || "P2".equals(upper) || "P3".equals(upper)) {
            return upper;
        }
        return "P2";
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be yyyy-MM-dd", ex);
        }
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime localDateTime) {
        return localDateTime == null ? null : Timestamp.valueOf(localDateTime);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
