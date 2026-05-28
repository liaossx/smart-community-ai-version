package com.lsx.ai.observability.service;

import com.lsx.ai.observability.dto.AiCallLogItem;
import com.lsx.ai.observability.dto.AiCallLogPageResponse;
import com.lsx.ai.observability.model.AiCallLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiCallLogService {
    private static final Logger log = LoggerFactory.getLogger(AiCallLogService.class);
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final String INSERT_LOG = String.join("\n",
            "INSERT INTO ai_call_log",
            "(request_id, biz_type, biz_key, provider, provider_version, model, status, latency_ms,",
            " confidence, request_summary, response_summary, retrieved_source_ids, error_message)",
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    );

    private final boolean enabled;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private volatile boolean tableMissing;

    public AiCallLogService(
            @Value("${smart-community.ai.observability.enabled:true}") boolean enabled,
            @Value("${smart-community.ai.observability.jdbc.url}") String jdbcUrl,
            @Value("${smart-community.ai.observability.jdbc.username}") String username,
            @Value("${smart-community.ai.observability.jdbc.password}") String password) {
        this.enabled = enabled;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public void record(AiCallLogEntry entry) {
        if (!enabled || tableMissing || entry == null) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(INSERT_LOG)) {
            statement.setString(1, entry.getRequestId());
            statement.setString(2, truncate(entry.getBizType(), 40));
            statement.setString(3, truncate(entry.getBizKey(), 128));
            statement.setString(4, truncate(entry.getProvider(), 40));
            statement.setString(5, truncate(entry.getProviderVersion(), 64));
            statement.setString(6, truncate(entry.getModel(), 100));
            statement.setString(7, truncate(defaultValue(entry.getStatus(), "SUCCESS"), 20));
            statement.setInt(8, entry.latencyMs());
            statement.setObject(9, entry.getConfidence());
            statement.setString(10, truncate(entry.getRequestSummary(), 1000));
            statement.setString(11, truncate(entry.getResponseSummary(), 1000));
            statement.setString(12, truncate(joinSourceIds(entry.getRetrievedSourceIds()), 1000));
            statement.setString(13, truncate(entry.getErrorMessage(), 2000));
            statement.executeUpdate();
        } catch (SQLException ex) {
            if (isMissingTable(ex)) {
                tableMissing = true;
                log.warn("AI call log table is missing. Execute sql/ai_call_log_schema_v1.sql and restart ai-service to enable call logs.");
                return;
            }
            log.warn("Failed to write AI call log. requestId={}", entry.getRequestId(), ex);
        }
    }

    public AiCallLogPageResponse list(String bizType, String status, Integer pageNum, Integer pageSize) {
        int safePageNum = safePageNum(pageNum);
        int safePageSize = safePageSize(pageSize);
        List<Object> params = new ArrayList<>();
        String whereSql = buildWhereSql(bizType, status, params);
        String countSql = "SELECT COUNT(*) FROM ai_call_log WHERE " + whereSql;
        String querySql = String.join("\n",
                "SELECT id, request_id, biz_type, biz_key, provider, provider_version, model, status,",
                "       latency_ms, confidence, request_summary, response_summary, retrieved_source_ids,",
                "       error_message, create_time",
                "FROM ai_call_log",
                "WHERE " + whereSql,
                "ORDER BY create_time DESC, id DESC",
                "LIMIT ? OFFSET ?"
        );

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            long total = count(connection, countSql, params);
            List<AiCallLogItem> records = queryPage(connection, querySql, params,
                    safePageSize, (safePageNum - 1) * safePageSize);
            AiCallLogPageResponse response = new AiCallLogPageResponse();
            response.setTotal(total);
            response.setPageNum(safePageNum);
            response.setPageSize(safePageSize);
            response.setRecords(records);
            return response;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "list AI call logs failed: " + ex.getMessage(), ex);
        }
    }

    private String buildWhereSql(String bizType, String status, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("1 = 1");
        if (StringUtils.hasText(bizType)) {
            conditions.add("biz_type = ?");
            params.add(bizType.trim().toUpperCase());
        }
        if (StringUtils.hasText(status)) {
            conditions.add("status = ?");
            params.add(status.trim().toUpperCase());
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

    private List<AiCallLogItem> queryPage(Connection connection, String querySql, List<Object> params,
                                          int pageSize, int offset) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(querySql)) {
            bindParams(statement, params);
            statement.setInt(params.size() + 1, pageSize);
            statement.setInt(params.size() + 2, offset);
            List<AiCallLogItem> records = new ArrayList<>();
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

    private AiCallLogItem toItem(ResultSet resultSet) throws SQLException {
        AiCallLogItem item = new AiCallLogItem();
        item.setId(resultSet.getLong("id"));
        item.setRequestId(resultSet.getString("request_id"));
        item.setBizType(resultSet.getString("biz_type"));
        item.setBizKey(resultSet.getString("biz_key"));
        item.setProvider(resultSet.getString("provider"));
        item.setProviderVersion(resultSet.getString("provider_version"));
        item.setModel(resultSet.getString("model"));
        item.setStatus(resultSet.getString("status"));
        item.setLatencyMs(resultSet.getObject("latency_ms", Integer.class));
        item.setConfidence(resultSet.getObject("confidence", Integer.class));
        item.setRequestSummary(resultSet.getString("request_summary"));
        item.setResponseSummary(resultSet.getString("response_summary"));
        item.setRetrievedSourceIds(resultSet.getString("retrieved_source_ids"));
        item.setErrorMessage(resultSet.getString("error_message"));
        Timestamp createTime = resultSet.getTimestamp("create_time");
        item.setCreateTime(createTime == null ? null : createTime.toLocalDateTime());
        return item;
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

    private String joinSourceIds(List<String> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return null;
        }
        return sourceIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String defaultValue(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private boolean isMissingTable(SQLException ex) {
        return ex.getErrorCode() == 1146
                || String.valueOf(ex.getMessage()).toLowerCase().contains("ai_call_log");
    }
}
