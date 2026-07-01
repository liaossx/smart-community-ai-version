package com.lsx.ai.customer.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "smart-community.ai.customer-service", name = "knowledge-store", havingValue = "jdbc")
public class JdbcCommunityKnowledgeRepository implements CommunityKnowledgeRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcCommunityKnowledgeRepository.class);

    /*
     * JDBC 知识库读取 SQL。
     *
     * RAG 的检索不是直接查 sys_notice，而是统一查 ai_knowledge_document/chunk。
     * 这样公告、制度、FAQ、流程都可以沉淀成同一种知识文档结构。
     */
    private static final String QUERY_ENABLED_CHUNKS = String.join("\n",
            "SELECT",
            "  d.source_id,",
            "  d.source_type,",
            "  d.community_id,",
            "  COALESCE(c.chunk_title, d.title) AS title,",
            "  c.chunk_content AS content,",
            "  COALESCE(c.keywords, d.keywords) AS keywords",
            "FROM ai_knowledge_document d",
            "JOIN ai_knowledge_chunk c ON c.document_id = d.id",
            "WHERE d.deleted = 0",
            "  AND d.status = 'ENABLED'",
            "  AND c.status = 'ENABLED'",
            "  AND d.visibility IN ('RESIDENT', 'ALL')",
            "  AND (d.effective_time IS NULL OR d.effective_time <= NOW())",
            "  AND (d.expire_time IS NULL OR d.expire_time > NOW())",
            "ORDER BY d.update_time DESC, d.id DESC, c.chunk_no ASC"
    );

    private final DataSource dataSource;

    public JdbcCommunityKnowledgeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<KnowledgeDocument> findAll() {
        // 这里是 RAG 的“知识加载”边界。后面接向量数据库时，通常替换这一层或 Retriever。
        List<KnowledgeDocument> documents = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(QUERY_ENABLED_CHUNKS);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                documents.add(toKnowledgeDocument(resultSet));
            }
        } catch (SQLException ex) {
            log.warn("Failed to load RAG knowledge from MySQL, return empty knowledge list", ex);
        }
        return documents;
    }

    private KnowledgeDocument toKnowledgeDocument(ResultSet resultSet) throws SQLException {
        // KnowledgeDocument 是检索器能理解的统一内存模型，屏蔽底层 MySQL 表结构。
        Long communityId = resultSet.getObject("community_id", Long.class);
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
        // keywords 在库里是字符串，进入检索阶段前转成数组，供关键词打分使用。
        if (!StringUtils.hasText(keywords)) {
            return List.of();
        }
        return Arrays.stream(keywords.split("[,，、\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }
}
