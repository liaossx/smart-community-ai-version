package com.lsx.ai.customer.knowledge;

import com.lsx.ai.knowledge.embedding.EmbeddingProvider;
import com.lsx.ai.knowledge.embedding.EmbeddingVector;
import com.lsx.ai.knowledge.embedding.VectorCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "smart-community.ai.customer-service", name = "knowledge-store", havingValue = "jdbc")
public class VectorCommunityKnowledgeRetriever {
    private static final Logger log = LoggerFactory.getLogger(VectorCommunityKnowledgeRetriever.class);

    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 5;
    private static final int MIN_TOP_VECTOR_SCORE_FOR_RELATIVE_FILTER = 35;
    private static final double MIN_RELATIVE_VECTOR_SCORE_RATIO = 0.72d;
    private static final double MIN_VECTOR_SIMILARITY = 0.18d;

    private static final String QUERY_ENABLED_EMBEDDINGS = String.join("\n",
            "SELECT",
            "  d.source_id,",
            "  d.source_type,",
            "  d.community_id,",
            "  COALESCE(c.chunk_title, d.title) AS title,",
            "  c.chunk_content AS content,",
            "  COALESCE(c.keywords, d.keywords) AS keywords,",
            "  e.embedding_vector",
            "FROM ai_knowledge_embedding e",
            "JOIN ai_knowledge_document d ON d.id = e.document_id",
            "JOIN ai_knowledge_chunk c ON c.id = e.chunk_id",
            "WHERE e.status = 'ENABLED'",
            "  AND e.embedding_provider = ?",
            "  AND e.embedding_model = ?",
            "  AND d.deleted = 0",
            "  AND d.status = 'ENABLED'",
            "  AND c.status = 'ENABLED'",
            "  AND d.visibility IN ('RESIDENT', 'ALL')",
            "  AND (d.effective_time IS NULL OR d.effective_time <= NOW())",
            "  AND (d.expire_time IS NULL OR d.expire_time > NOW())",
            "ORDER BY d.update_time DESC, d.id DESC, c.chunk_no ASC"
    );

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final EmbeddingProvider embeddingProvider;
    private final VectorCodec vectorCodec;

    public VectorCommunityKnowledgeRetriever(
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

    public List<RetrievedKnowledgeDocument> retrieve(String question, Long communityId, Integer topK) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        EmbeddingVector queryVector = embeddingProvider.embed(question);
        if (queryVector.dimension() <= 0) {
            return List.of();
        }
        int limit = safeTopK(topK);
        List<RetrievedKnowledgeDocument> results = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(QUERY_ENABLED_EMBEDDINGS)) {
            statement.setString(1, embeddingProvider.provider());
            statement.setString(2, embeddingProvider.model());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Long documentCommunityId = resultSet.getObject("community_id", Long.class);
                    if (!matchesCommunity(documentCommunityId, communityId)) {
                        continue;
                    }
                    EmbeddingVector storedVector = vectorCodec.decode(resultSet.getString("embedding_vector"));
                    double similarity = queryVector.cosineSimilarity(storedVector);
                    if (similarity < MIN_VECTOR_SIMILARITY) {
                        continue;
                    }
                    int vectorScore = toVectorScore(similarity);
                    KnowledgeDocument document = toKnowledgeDocument(resultSet, documentCommunityId);
                    results.add(new RetrievedKnowledgeDocument(
                            document,
                            toRetrievalScore(vectorScore),
                            0,
                            vectorScore,
                            "VECTOR"
                    ));
                }
            }
        } catch (SQLException ex) {
            log.warn("Failed to retrieve vector knowledge from MySQL, return empty vector results", ex);
            return List.of();
        }
        return filterLowRelevance(results).stream()
                .sorted(Comparator.comparingInt(RetrievedKnowledgeDocument::getVectorScore).reversed()
                        .thenComparing(Comparator.comparingInt(RetrievedKnowledgeDocument::getScore).reversed()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<RetrievedKnowledgeDocument> filterLowRelevance(List<RetrievedKnowledgeDocument> results) {
        if (results.isEmpty()) {
            return results;
        }
        int bestVectorScore = results.stream()
                .mapToInt(RetrievedKnowledgeDocument::getVectorScore)
                .max()
                .orElse(0);
        if (bestVectorScore < MIN_TOP_VECTOR_SCORE_FOR_RELATIVE_FILTER) {
            return results;
        }
        int minAllowedScore = (int) Math.ceil(bestVectorScore * MIN_RELATIVE_VECTOR_SCORE_RATIO);
        return results.stream()
                .filter(result -> result.getVectorScore() >= minAllowedScore)
                .collect(Collectors.toList());
    }

    private boolean matchesCommunity(Long documentCommunityId, Long communityId) {
        return documentCommunityId == null
                || communityId == null
                || documentCommunityId.equals(communityId);
    }

    private KnowledgeDocument toKnowledgeDocument(ResultSet resultSet, Long communityId) throws SQLException {
        return new KnowledgeDocument(
                resultSet.getString("source_id"),
                resultSet.getString("source_type"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                splitKeywords(resultSet.getString("keywords")),
                communityId
        );
    }

    private List<String> splitKeywords(String keywords) {
        if (!StringUtils.hasText(keywords)) {
            return List.of();
        }
        return Arrays.stream(keywords.split("[,，、\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private int toVectorScore(double similarity) {
        return (int) Math.round(Math.max(similarity, 0.0d) * 100);
    }

    private int toRetrievalScore(int vectorScore) {
        return Math.max(1, Math.round(vectorScore * 0.35f));
    }

    private int safeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }
}
