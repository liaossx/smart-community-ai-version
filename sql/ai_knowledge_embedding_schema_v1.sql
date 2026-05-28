SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- AI knowledge embedding table
-- ------------------------------------------------------------
-- Stores one embedding vector for each ai_knowledge_chunk row.
-- v1 stores vectors as text so it can run on normal MySQL without a vector
-- extension. Later this table can be replaced by Milvus, pgvector, Elasticsearch,
-- Redis Vector, or a MySQL vector column.
CREATE TABLE IF NOT EXISTS `ai_knowledge_embedding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'embedding id',
  `document_id` bigint NOT NULL COMMENT 'ai_knowledge_document.id',
  `chunk_id` bigint NOT NULL COMMENT 'ai_knowledge_chunk.id',
  `source_type` varchar(32) NOT NULL COMMENT 'knowledge source type',
  `source_id` varchar(64) NOT NULL COMMENT 'knowledge source id',
  `community_id` bigint NULL DEFAULT NULL COMMENT 'community id, null means shared knowledge',
  `embedding_provider` varchar(40) NOT NULL COMMENT 'HASH/OPENAI/QWEN/BGE etc',
  `embedding_model` varchar(100) NOT NULL COMMENT 'embedding model name',
  `embedding_dimension` int NOT NULL COMMENT 'vector dimension',
  `embedding_vector` mediumtext NOT NULL COMMENT 'serialized vector values',
  `content_hash` varchar(64) NULL DEFAULT NULL COMMENT 'chunk content hash when embedding was generated',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_chunk_provider_model` (`chunk_id`, `embedding_provider`, `embedding_model`) USING BTREE,
  KEY `idx_source` (`source_type`, `source_id`) USING BTREE,
  KEY `idx_community_status` (`community_id`, `status`) USING BTREE,
  KEY `idx_document_id` (`document_id`) USING BTREE,
  CONSTRAINT `fk_ai_knowledge_embedding_document`
    FOREIGN KEY (`document_id`) REFERENCES `ai_knowledge_document` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ai_knowledge_embedding_chunk`
    FOREIGN KEY (`chunk_id`) REFERENCES `ai_knowledge_chunk` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'AI RAG knowledge embedding table'
  ROW_FORMAT = DYNAMIC;
