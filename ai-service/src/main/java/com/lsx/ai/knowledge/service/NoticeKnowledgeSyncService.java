package com.lsx.ai.knowledge.service;

import com.lsx.ai.knowledge.dto.KnowledgeSyncResponse;
import com.lsx.ai.knowledge.model.NoticeKnowledgeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class NoticeKnowledgeSyncService {
    private static final Logger log = LoggerFactory.getLogger(NoticeKnowledgeSyncService.class);

    private static final String SOURCE_TYPE = "COMMUNITY_NOTICE";
    private static final String ORIGIN_TABLE = "sys_notice";

    private static final String QUERY_PUBLISHED_NOTICES = String.join("\n",
            "SELECT",
            "  id, community_id, community_name, title, content, target_type, target_building,",
            "  publish_time, expire_time",
            "FROM sys_notice",
            "WHERE deleted = 0",
            "  AND publish_status = 'PUBLISHED'",
            "  AND (expire_time IS NULL OR expire_time > NOW())",
            "ORDER BY update_time DESC, id DESC"
    );

    private static final String UPSERT_DOCUMENT = String.join("\n",
            "INSERT INTO ai_knowledge_document",
            "(source_type, source_id, community_id, title, content, keywords, status, visibility,",
            " effective_time, expire_time, origin_table, origin_id, content_hash, version)",
            "VALUES (?, ?, ?, ?, ?, ?, 'ENABLED', 'RESIDENT', ?, ?, ?, ?, ?, 1)",
            "ON DUPLICATE KEY UPDATE",
            "  id = LAST_INSERT_ID(id),",
            "  community_id = VALUES(community_id),",
            "  title = VALUES(title),",
            "  content = VALUES(content),",
            "  keywords = VALUES(keywords),",
            "  status = 'ENABLED',",
            "  visibility = 'RESIDENT',",
            "  effective_time = VALUES(effective_time),",
            "  expire_time = VALUES(expire_time),",
            "  origin_table = VALUES(origin_table),",
            "  origin_id = VALUES(origin_id),",
            "  version = IF(content_hash IS NULL OR content_hash <> VALUES(content_hash), version + 1, version),",
            "  content_hash = VALUES(content_hash),",
            "  update_time = CURRENT_TIMESTAMP"
    );

    private static final String UPSERT_CHUNK = String.join("\n",
            "INSERT INTO ai_knowledge_chunk",
            "(document_id, chunk_no, chunk_title, chunk_content, keywords, char_count, content_hash, status)",
            "VALUES (?, 1, ?, ?, ?, CHAR_LENGTH(?), ?, 'ENABLED')",
            "ON DUPLICATE KEY UPDATE",
            "  chunk_title = VALUES(chunk_title),",
            "  chunk_content = VALUES(chunk_content),",
            "  keywords = VALUES(keywords),",
            "  char_count = VALUES(char_count),",
            "  content_hash = VALUES(content_hash),",
            "  status = 'ENABLED',",
            "  update_time = CURRENT_TIMESTAMP"
    );

    private static final String DISABLE_INVALID_NOTICE_DOCUMENTS = String.join("\n",
            "UPDATE ai_knowledge_document d",
            "LEFT JOIN sys_notice n ON n.id = d.origin_id",
            "SET d.status = 'DISABLED', d.update_time = CURRENT_TIMESTAMP",
            "WHERE d.origin_table = 'sys_notice'",
            "  AND d.origin_id IS NOT NULL",
            "  AND d.source_type = 'COMMUNITY_NOTICE'",
            "  AND d.deleted = 0",
            "  AND d.status = 'ENABLED'",
            "  AND (n.id IS NULL",
            "       OR n.deleted <> 0",
            "       OR n.publish_status <> 'PUBLISHED'",
            "       OR (n.expire_time IS NOT NULL AND n.expire_time <= NOW()))"
    );

    private static final String DISABLE_CHUNKS_FOR_DISABLED_DOCUMENTS = String.join("\n",
            "UPDATE ai_knowledge_chunk c",
            "JOIN ai_knowledge_document d ON d.id = c.document_id",
            "SET c.status = 'DISABLED', c.update_time = CURRENT_TIMESTAMP",
            "WHERE d.origin_table = 'sys_notice'",
            "  AND d.source_type = 'COMMUNITY_NOTICE'",
            "  AND d.status = 'DISABLED'",
            "  AND c.status = 'ENABLED'"
    );

    private static final String INSERT_SYNC_LOG = String.join("\n",
            "INSERT INTO ai_knowledge_sync_log",
            "(sync_batch_no, source_type, source_id, document_id, sync_action, sync_status, message)",
            "VALUES (?, ?, ?, ?, ?, ?, ?)"
    );

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final NoticeKnowledgeKeywordExtractor keywordExtractor;

    public NoticeKnowledgeSyncService(
            @Value("${smart-community.ai.customer-service.jdbc.url}") String jdbcUrl,
            @Value("${smart-community.ai.customer-service.jdbc.username}") String username,
            @Value("${smart-community.ai.customer-service.jdbc.password}") String password,
            NoticeKnowledgeKeywordExtractor keywordExtractor) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.keywordExtractor = keywordExtractor;
    }

    public KnowledgeSyncResponse syncPublishedNotices() {
        String batchNo = "notice-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        KnowledgeSyncResponse response = new KnowledgeSyncResponse();
        response.setSyncBatchNo(batchNo);
        response.setScannedCount(0);
        response.setSyncedCount(0);
        response.setDisabledCount(0);
        response.setFailedCount(0);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            connection.setAutoCommit(false);
            try {
                List<NoticeKnowledgeRecord> notices = queryPublishedNotices(connection);
                response.setScannedCount(notices.size());
                for (NoticeKnowledgeRecord notice : notices) {
                    try {
                        long documentId = upsertNoticeKnowledge(connection, notice);
                        insertLog(connection, batchNo, sourceId(notice), documentId,
                                "UPDATE", "SUCCESS", "notice synced to RAG knowledge");
                        response.setSyncedCount(response.getSyncedCount() + 1);
                    } catch (SQLException ex) {
                        response.setFailedCount(response.getFailedCount() + 1);
                        response.getMessages().add("notice " + notice.getId() + " sync failed: " + ex.getMessage());
                        insertLog(connection, batchNo, sourceId(notice), null,
                                "UPDATE", "FAILED", ex.getMessage());
                    }
                }
                int disabled = disableInvalidNoticeKnowledge(connection);
                response.setDisabledCount(disabled);
                if (disabled > 0) {
                    insertLog(connection, batchNo, "*", null,
                            "DISABLE", "SUCCESS", "disabled invalid notice knowledge: " + disabled);
                }
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            log.warn("Notice knowledge sync failed", ex);
            response.setFailedCount(response.getFailedCount() + 1);
            response.getMessages().add("sync failed: " + ex.getMessage());
        }
        return response;
    }

    private List<NoticeKnowledgeRecord> queryPublishedNotices(Connection connection) throws SQLException {
        List<NoticeKnowledgeRecord> notices = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(QUERY_PUBLISHED_NOTICES);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                notices.add(toNotice(resultSet));
            }
        }
        return notices;
    }

    private NoticeKnowledgeRecord toNotice(ResultSet resultSet) throws SQLException {
        NoticeKnowledgeRecord notice = new NoticeKnowledgeRecord();
        notice.setId(resultSet.getLong("id"));
        notice.setCommunityId(resultSet.getObject("community_id", Long.class));
        notice.setCommunityName(resultSet.getString("community_name"));
        notice.setTitle(resultSet.getString("title"));
        notice.setContent(resultSet.getString("content"));
        notice.setTargetType(resultSet.getString("target_type"));
        notice.setTargetBuilding(resultSet.getString("target_building"));
        notice.setPublishTime(toLocalDateTime(resultSet.getTimestamp("publish_time")));
        notice.setExpireTime(toLocalDateTime(resultSet.getTimestamp("expire_time")));
        return notice;
    }

    private long upsertNoticeKnowledge(Connection connection, NoticeKnowledgeRecord notice) throws SQLException {
        String keywords = keywordExtractor.extract(notice);
        String contentHash = sha256(notice.getTitle() + "\n" + notice.getContent() + "\n" + keywords);
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_DOCUMENT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, SOURCE_TYPE);
            statement.setString(2, sourceId(notice));
            statement.setObject(3, notice.getCommunityId());
            statement.setString(4, notice.getTitle());
            statement.setString(5, notice.getContent());
            statement.setString(6, keywords);
            statement.setTimestamp(7, toTimestamp(notice.getPublishTime()));
            statement.setTimestamp(8, toTimestamp(notice.getExpireTime()));
            statement.setString(9, ORIGIN_TABLE);
            statement.setLong(10, notice.getId());
            statement.setString(11, contentHash);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long documentId = keys.getLong(1);
                    upsertChunk(connection, documentId, notice, keywords, contentHash);
                    return documentId;
                }
            }
        }
        throw new SQLException("failed to resolve upserted document id");
    }

    private void upsertChunk(Connection connection, long documentId, NoticeKnowledgeRecord notice,
                             String keywords, String contentHash) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_CHUNK)) {
            statement.setLong(1, documentId);
            statement.setString(2, notice.getTitle());
            statement.setString(3, notice.getContent());
            statement.setString(4, keywords);
            statement.setString(5, notice.getContent());
            statement.setString(6, contentHash);
            statement.executeUpdate();
        }
    }

    private int disableInvalidNoticeKnowledge(Connection connection) throws SQLException {
        int disabledDocuments;
        try (PreparedStatement statement = connection.prepareStatement(DISABLE_INVALID_NOTICE_DOCUMENTS)) {
            disabledDocuments = statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(DISABLE_CHUNKS_FOR_DISABLED_DOCUMENTS)) {
            statement.executeUpdate();
        }
        return disabledDocuments;
    }

    private void insertLog(Connection connection, String batchNo, String sourceId, Long documentId,
                           String action, String status, String message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SYNC_LOG)) {
            statement.setString(1, batchNo);
            statement.setString(2, SOURCE_TYPE);
            statement.setString(3, sourceId);
            statement.setObject(4, documentId);
            statement.setString(5, action);
            statement.setString(6, status);
            statement.setString(7, truncate(message, 1000));
            statement.executeUpdate();
        }
    }

    private String sourceId(NoticeKnowledgeRecord notice) {
        return "sys_notice:" + notice.getId();
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

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
