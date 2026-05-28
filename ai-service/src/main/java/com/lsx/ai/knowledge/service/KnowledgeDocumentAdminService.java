package com.lsx.ai.knowledge.service;

import com.lsx.ai.knowledge.dto.KnowledgeDocumentItem;
import com.lsx.ai.knowledge.dto.KnowledgeDocumentMutationResponse;
import com.lsx.ai.knowledge.dto.KnowledgeDocumentPageResponse;
import com.lsx.ai.knowledge.dto.KnowledgeDocumentSaveRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KnowledgeDocumentAdminService {
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private static final String INSERT_DOCUMENT = String.join("\n",
            "INSERT INTO ai_knowledge_document",
            "(source_type, source_id, community_id, title, content, keywords, status, visibility,",
            " effective_time, expire_time, content_hash, version)",
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)"
    );

    private static final String UPDATE_DOCUMENT = String.join("\n",
            "UPDATE ai_knowledge_document",
            "SET source_type = ?, source_id = ?, community_id = ?, title = ?, content = ?, keywords = ?,",
            "    status = ?, visibility = ?, effective_time = ?, expire_time = ?,",
            "    version = IF(content_hash IS NULL OR content_hash <> ?, version + 1, version),",
            "    content_hash = ?, update_time = CURRENT_TIMESTAMP",
            "WHERE id = ? AND deleted = 0"
    );

    private static final String UPSERT_CHUNK = String.join("\n",
            "INSERT INTO ai_knowledge_chunk",
            "(document_id, chunk_no, chunk_title, chunk_content, keywords, char_count, content_hash, status)",
            "VALUES (?, 1, ?, ?, ?, CHAR_LENGTH(?), ?, ?)",
            "ON DUPLICATE KEY UPDATE",
            "  chunk_title = VALUES(chunk_title),",
            "  chunk_content = VALUES(chunk_content),",
            "  keywords = VALUES(keywords),",
            "  char_count = VALUES(char_count),",
            "  content_hash = VALUES(content_hash),",
            "  status = VALUES(status),",
            "  update_time = CURRENT_TIMESTAMP"
    );

    private static final String FIND_DOCUMENT_BY_ID = String.join("\n",
            "SELECT id, source_type, source_id, community_id, title, content, keywords, status, visibility,",
            "       effective_time, expire_time, origin_table, origin_id, version, create_time, update_time",
            "FROM ai_knowledge_document",
            "WHERE id = ? AND deleted = 0"
    );

    private static final String DISABLE_DOCUMENT = String.join("\n",
            "UPDATE ai_knowledge_document",
            "SET status = 'DISABLED', update_time = CURRENT_TIMESTAMP",
            "WHERE id = ? AND deleted = 0"
    );

    private static final String DISABLE_CHUNK = String.join("\n",
            "UPDATE ai_knowledge_chunk",
            "SET status = 'DISABLED', update_time = CURRENT_TIMESTAMP",
            "WHERE document_id = ?"
    );

    private static final String INSERT_SYNC_LOG = String.join("\n",
            "INSERT INTO ai_knowledge_sync_log",
            "(sync_batch_no, source_type, source_id, document_id, sync_action, sync_status, message)",
            "VALUES (?, ?, ?, ?, ?, ?, ?)"
    );

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public KnowledgeDocumentAdminService(
            @Value("${smart-community.ai.customer-service.jdbc.url}") String jdbcUrl,
            @Value("${smart-community.ai.customer-service.jdbc.username}") String username,
            @Value("${smart-community.ai.customer-service.jdbc.password}") String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public KnowledgeDocumentMutationResponse create(KnowledgeDocumentSaveRequest request) {
        String sourceType = normalizeSourceType(request.getSourceType());
        String sourceId = StringUtils.hasText(request.getSourceId())
                ? request.getSourceId().trim()
                : "manual:" + UUID.randomUUID().toString().replace("-", "");
        String status = normalizeStatus(request.getStatus());
        String visibility = normalizeVisibility(request.getVisibility());
        String keywords = normalizeKeywords(request.getKeywords());
        String contentHash = sha256(request.getTitle() + "\n" + request.getContent() + "\n" + keywords);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            connection.setAutoCommit(false);
            try {
                long documentId;
                try (PreparedStatement statement = connection.prepareStatement(INSERT_DOCUMENT, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, sourceType);
                    statement.setString(2, sourceId);
                    statement.setObject(3, request.getCommunityId());
                    statement.setString(4, request.getTitle());
                    statement.setString(5, request.getContent());
                    statement.setString(6, keywords);
                    statement.setString(7, status);
                    statement.setString(8, visibility);
                    statement.setTimestamp(9, toTimestamp(request.getEffectiveTime()));
                    statement.setTimestamp(10, toTimestamp(request.getExpireTime()));
                    statement.setString(11, contentHash);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("failed to resolve created document id");
                        }
                        documentId = keys.getLong(1);
                    }
                }
                upsertChunk(connection, documentId, request.getTitle(), request.getContent(), keywords, contentHash, chunkStatus(status));
                insertLog(connection, "manual", sourceType, sourceId, documentId,
                        "CREATE", "SUCCESS", "manual knowledge document created");
                connection.commit();
                return mutationResponse(documentId, sourceType, sourceId, status, "created");
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "create knowledge document failed: " + ex.getMessage(), ex);
        }
    }

    public KnowledgeDocumentMutationResponse update(Long id, KnowledgeDocumentSaveRequest request) {
        KnowledgeDocumentItem existing = getRequired(id);
        String sourceType = normalizeSourceType(request.getSourceType());
        String sourceId = StringUtils.hasText(request.getSourceId()) ? request.getSourceId().trim() : existing.getSourceId();
        String status = normalizeStatus(request.getStatus());
        String visibility = normalizeVisibility(request.getVisibility());
        String keywords = normalizeKeywords(request.getKeywords());
        String contentHash = sha256(request.getTitle() + "\n" + request.getContent() + "\n" + keywords);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_DOCUMENT)) {
                statement.setString(1, sourceType);
                statement.setString(2, sourceId);
                statement.setObject(3, request.getCommunityId());
                statement.setString(4, request.getTitle());
                statement.setString(5, request.getContent());
                statement.setString(6, keywords);
                statement.setString(7, status);
                statement.setString(8, visibility);
                statement.setTimestamp(9, toTimestamp(request.getEffectiveTime()));
                statement.setTimestamp(10, toTimestamp(request.getExpireTime()));
                statement.setString(11, contentHash);
                statement.setString(12, contentHash);
                statement.setLong(13, id);
                int updated = statement.executeUpdate();
                if (updated == 0) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "knowledge document not found: " + id);
                }
                upsertChunk(connection, id, request.getTitle(), request.getContent(), keywords, contentHash, chunkStatus(status));
                insertLog(connection, "manual", sourceType, sourceId, id,
                        "UPDATE", "SUCCESS", "manual knowledge document updated");
                connection.commit();
                return mutationResponse(id, sourceType, sourceId, status, "updated");
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "update knowledge document failed: " + ex.getMessage(), ex);
        }
    }

    public KnowledgeDocumentMutationResponse disable(Long id) {
        KnowledgeDocumentItem existing = getRequired(id);
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(DISABLE_DOCUMENT)) {
                    statement.setLong(1, id);
                    int updated = statement.executeUpdate();
                    if (updated == 0) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "knowledge document not found: " + id);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(DISABLE_CHUNK)) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                insertLog(connection, "manual", existing.getSourceType(), existing.getSourceId(), id,
                        "DISABLE", "SUCCESS", "manual knowledge document disabled");
                connection.commit();
                return mutationResponse(id, existing.getSourceType(), existing.getSourceId(), "DISABLED", "disabled");
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "disable knowledge document failed: " + ex.getMessage(), ex);
        }
    }

    public KnowledgeDocumentItem getRequired(Long id) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(FIND_DOCUMENT_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return toItem(resultSet);
                }
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "load knowledge document failed: " + ex.getMessage(), ex);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "knowledge document not found: " + id);
    }

    public KnowledgeDocumentPageResponse list(String keyword, String sourceType, String status,
                                              Long communityId, Integer pageNum, Integer pageSize) {
        int safePageNum = safePageNum(pageNum);
        int safePageSize = safePageSize(pageSize);
        List<Object> params = new ArrayList<>();
        String whereSql = buildWhereSql(keyword, sourceType, status, communityId, params);
        String countSql = "SELECT COUNT(*) FROM ai_knowledge_document WHERE " + whereSql;
        String querySql = String.join("\n",
                "SELECT id, source_type, source_id, community_id, title, content, keywords, status, visibility,",
                "       effective_time, expire_time, origin_table, origin_id, version, create_time, update_time",
                "FROM ai_knowledge_document",
                "WHERE " + whereSql,
                "ORDER BY update_time DESC, id DESC",
                "LIMIT ? OFFSET ?"
        );

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            long total = count(connection, countSql, params);
            List<KnowledgeDocumentItem> records = queryPage(connection, querySql, params,
                    safePageSize, (safePageNum - 1) * safePageSize);
            KnowledgeDocumentPageResponse response = new KnowledgeDocumentPageResponse();
            response.setTotal(total);
            response.setPageNum(safePageNum);
            response.setPageSize(safePageSize);
            response.setRecords(records);
            return response;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "list knowledge documents failed: " + ex.getMessage(), ex);
        }
    }

    private String buildWhereSql(String keyword, String sourceType, String status, Long communityId, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("deleted = 0");
        if (StringUtils.hasText(keyword)) {
            conditions.add("(title LIKE ? OR content LIKE ? OR keywords LIKE ? OR source_id LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (StringUtils.hasText(sourceType)) {
            conditions.add("source_type = ?");
            params.add(normalizeSourceType(sourceType));
        }
        if (StringUtils.hasText(status)) {
            conditions.add("status = ?");
            params.add(normalizeStatus(status));
        }
        if (communityId != null) {
            conditions.add("(community_id = ? OR community_id IS NULL)");
            params.add(communityId);
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

    private List<KnowledgeDocumentItem> queryPage(Connection connection, String querySql, List<Object> params,
                                                  int pageSize, int offset) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(querySql)) {
            bindParams(statement, params);
            statement.setInt(params.size() + 1, pageSize);
            statement.setInt(params.size() + 2, offset);
            List<KnowledgeDocumentItem> records = new ArrayList<>();
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

    private void upsertChunk(Connection connection, long documentId, String title, String content,
                             String keywords, String contentHash, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_CHUNK)) {
            statement.setLong(1, documentId);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.setString(4, keywords);
            statement.setString(5, content);
            statement.setString(6, contentHash);
            statement.setString(7, status);
            statement.executeUpdate();
        }
    }

    private void insertLog(Connection connection, String batchNo, String sourceType, String sourceId, Long documentId,
                           String action, String status, String message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SYNC_LOG)) {
            statement.setString(1, batchNo);
            statement.setString(2, sourceType);
            statement.setString(3, sourceId);
            statement.setObject(4, documentId);
            statement.setString(5, action);
            statement.setString(6, status);
            statement.setString(7, message);
            statement.executeUpdate();
        }
    }

    private KnowledgeDocumentItem toItem(ResultSet resultSet) throws SQLException {
        KnowledgeDocumentItem item = new KnowledgeDocumentItem();
        item.setId(resultSet.getLong("id"));
        item.setSourceType(resultSet.getString("source_type"));
        item.setSourceId(resultSet.getString("source_id"));
        item.setCommunityId(resultSet.getObject("community_id", Long.class));
        item.setTitle(resultSet.getString("title"));
        item.setContent(resultSet.getString("content"));
        item.setKeywords(resultSet.getString("keywords"));
        item.setStatus(resultSet.getString("status"));
        item.setVisibility(resultSet.getString("visibility"));
        item.setEffectiveTime(toLocalDateTime(resultSet.getTimestamp("effective_time")));
        item.setExpireTime(toLocalDateTime(resultSet.getTimestamp("expire_time")));
        item.setOriginTable(resultSet.getString("origin_table"));
        item.setOriginId(resultSet.getObject("origin_id", Long.class));
        item.setVersion(resultSet.getInt("version"));
        item.setCreateTime(toLocalDateTime(resultSet.getTimestamp("create_time")));
        item.setUpdateTime(toLocalDateTime(resultSet.getTimestamp("update_time")));
        return item;
    }

    private KnowledgeDocumentMutationResponse mutationResponse(Long id, String sourceType, String sourceId,
                                                               String status, String message) {
        KnowledgeDocumentMutationResponse response = new KnowledgeDocumentMutationResponse();
        response.setId(id);
        response.setSourceType(sourceType);
        response.setSourceId(sourceId);
        response.setStatus(status);
        response.setMessage(message);
        return response;
    }

    private String normalizeSourceType(String sourceType) {
        return sourceType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ENABLED";
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if ("ENABLED".equals(value) || "DISABLED".equals(value) || "DRAFT".equals(value)) {
            return value;
        }
        return "ENABLED";
    }

    private String normalizeVisibility(String visibility) {
        if (!StringUtils.hasText(visibility)) {
            return "RESIDENT";
        }
        String value = visibility.trim().toUpperCase(Locale.ROOT);
        if ("RESIDENT".equals(value) || "STAFF".equals(value) || "ALL".equals(value)) {
            return value;
        }
        return "RESIDENT";
    }

    private String chunkStatus(String documentStatus) {
        return "ENABLED".equals(documentStatus) ? "ENABLED" : "DISABLED";
    }

    private String normalizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }
        return keywords.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime localDateTime) {
        return localDateTime == null ? null : Timestamp.valueOf(localDateTime);
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
