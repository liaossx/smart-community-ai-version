package com.lsx.ai.knowledge.service;

import com.lsx.ai.knowledge.dto.KnowledgeEmbeddingRebuildResponse;
import com.lsx.ai.knowledge.embedding.EmbeddingProvider;
import com.lsx.ai.knowledge.embedding.EmbeddingVector;
import com.lsx.ai.knowledge.embedding.VectorCodec;
import com.lsx.ai.knowledge.model.KnowledgeChunkForEmbedding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeEmbeddingRebuildService {
    private static final String QUERY_ENABLED_CHUNKS = String.join("\n",
            "SELECT",
            "  d.id AS document_id,",
            "  c.id AS chunk_id,",
            "  d.source_type,",
            "  d.source_id,",
            "  d.community_id,",
            "  COALESCE(c.chunk_title, d.title) AS title,",
            "  c.chunk_content AS content,",
            "  COALESCE(c.keywords, d.keywords) AS keywords,",
            "  COALESCE(c.content_hash, d.content_hash) AS content_hash",
            "FROM ai_knowledge_document d",
            "JOIN ai_knowledge_chunk c ON c.document_id = d.id",
            "WHERE d.deleted = 0",
            "  AND d.status = 'ENABLED'",
            "  AND c.status = 'ENABLED'",
            "ORDER BY d.id ASC, c.chunk_no ASC"
    );

    private static final String UPSERT_EMBEDDING = String.join("\n",
            "INSERT INTO ai_knowledge_embedding",
            "(document_id, chunk_id, source_type, source_id, community_id, embedding_provider, embedding_model,",
            " embedding_dimension, embedding_vector, content_hash, status)",
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ENABLED')",
            "ON DUPLICATE KEY UPDATE",
            "  source_type = VALUES(source_type),",
            "  source_id = VALUES(source_id),",
            "  community_id = VALUES(community_id),",
            "  embedding_dimension = VALUES(embedding_dimension),",
            "  embedding_vector = VALUES(embedding_vector),",
            "  content_hash = VALUES(content_hash),",
            "  status = 'ENABLED',",
            "  update_time = CURRENT_TIMESTAMP"
    );

    private static final String DISABLE_STALE_EMBEDDINGS = String.join("\n",
            "UPDATE ai_knowledge_embedding e",
            "LEFT JOIN ai_knowledge_chunk c ON c.id = e.chunk_id",
            "LEFT JOIN ai_knowledge_document d ON d.id = e.document_id",
            "SET e.status = 'DISABLED', e.update_time = CURRENT_TIMESTAMP",
            "WHERE e.embedding_provider = ?",
            "  AND e.embedding_model = ?",
            "  AND (c.id IS NULL OR d.id IS NULL OR c.status <> 'ENABLED' OR d.status <> 'ENABLED' OR d.deleted <> 0)"
    );

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final EmbeddingProvider embeddingProvider;
    private final VectorCodec vectorCodec;

    public KnowledgeEmbeddingRebuildService(
            @Value("${smart-community.ai.customer-service.jdbc.url}") String jdbcUrl,
            @Value("${smart-community.ai.customer-service.jdbc.username}") String username,
            @Value("${smart-community.ai.customer-service.jdbc.password}") String password,
            EmbeddingProvider embeddingProvider,
            VectorCodec vectorCodec) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.embeddingProvider = embeddingProvider;
        this.vectorCodec = vectorCodec;
    }

    public KnowledgeEmbeddingRebuildResponse rebuildAll() {
        String batchNo = "embedding-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        KnowledgeEmbeddingRebuildResponse response = baseResponse(batchNo);
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            List<KnowledgeChunkForEmbedding> chunks = loadEnabledChunks(connection);
            response.setScannedCount(chunks.size());
            int embeddedCount = 0;
            int failedCount = 0;
            for (KnowledgeChunkForEmbedding chunk : chunks) {
                try {
                    EmbeddingVector vector = embeddingProvider.embed(chunk.embeddingText());
                    upsertEmbedding(connection, chunk, vector);
                    embeddedCount++;
                } catch (RuntimeException | SQLException ex) {
                    failedCount++;
                    response.getMessages().add("chunk " + chunk.getChunkId() + " failed: " + ex.getMessage());
                }
            }
            int disabledCount = disableStaleEmbeddings(connection);
            response.setEmbeddedCount(embeddedCount);
            response.setFailedCount(failedCount);
            response.setSkippedCount(disabledCount);
            if (disabledCount > 0) {
                response.getMessages().add("disabled stale embeddings: " + disabledCount);
            }
            return response;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "rebuild knowledge embeddings failed: " + ex.getMessage(), ex);
        }
    }

    private KnowledgeEmbeddingRebuildResponse baseResponse(String batchNo) {
        KnowledgeEmbeddingRebuildResponse response = new KnowledgeEmbeddingRebuildResponse();
        response.setRebuildBatchNo(batchNo);
        response.setScannedCount(0);
        response.setEmbeddedCount(0);
        response.setSkippedCount(0);
        response.setFailedCount(0);
        response.setProvider(embeddingProvider.provider());
        response.setModel(embeddingProvider.model());
        response.setDimension(embeddingProvider.dimension());
        return response;
    }

    private List<KnowledgeChunkForEmbedding> loadEnabledChunks(Connection connection) throws SQLException {
        List<KnowledgeChunkForEmbedding> chunks = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(QUERY_ENABLED_CHUNKS);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                KnowledgeChunkForEmbedding chunk = new KnowledgeChunkForEmbedding();
                chunk.setDocumentId(resultSet.getLong("document_id"));
                chunk.setChunkId(resultSet.getLong("chunk_id"));
                chunk.setSourceType(resultSet.getString("source_type"));
                chunk.setSourceId(resultSet.getString("source_id"));
                chunk.setCommunityId(resultSet.getObject("community_id", Long.class));
                chunk.setTitle(resultSet.getString("title"));
                chunk.setContent(resultSet.getString("content"));
                chunk.setKeywords(resultSet.getString("keywords"));
                chunk.setContentHash(resultSet.getString("content_hash"));
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    private void upsertEmbedding(Connection connection, KnowledgeChunkForEmbedding chunk, EmbeddingVector vector)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_EMBEDDING)) {
            statement.setLong(1, chunk.getDocumentId());
            statement.setLong(2, chunk.getChunkId());
            statement.setString(3, chunk.getSourceType());
            statement.setString(4, chunk.getSourceId());
            statement.setObject(5, chunk.getCommunityId());
            statement.setString(6, embeddingProvider.provider());
            statement.setString(7, embeddingProvider.model());
            statement.setInt(8, vector.dimension());
            statement.setString(9, vectorCodec.encode(vector));
            statement.setString(10, chunk.getContentHash());
            statement.executeUpdate();
        }
    }

    private int disableStaleEmbeddings(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DISABLE_STALE_EMBEDDINGS)) {
            statement.setString(1, embeddingProvider.provider());
            statement.setString(2, embeddingProvider.model());
            return statement.executeUpdate();
        }
    }
}
