# 向量 RAG 第一步：Embedding 入库

这一阶段完成的是：

```text
ai_knowledge_chunk 文本块
 -> EmbeddingProvider 生成向量
 -> ai_knowledge_embedding 保存向量
```

它是向量检索的前置工程链路。下一阶段才会把问答检索从关键词检索升级为向量召回/混合召回。

## 为什么先做这一步

RAG 的完整链路是：

```text
知识来源
 -> 清洗入库
 -> 文本切块 chunk
 -> embedding 向量化
 -> 向量检索
 -> Prompt 拼接
 -> LLM 回答
 -> 引用来源和观测日志
```

当前项目已经完成：

- 知识来源入库：公告、制度、流程、FAQ。
- 文本切块：`ai_knowledge_chunk`。
- RAG 问答：目前是关键词检索版。
- 调用日志：`ai_call_log`。

本阶段补齐的是：

- 向量表：`ai_knowledge_embedding`。
- Embedding Provider 抽象。
- 本地 Hash Embedding 默认实现。
- 重建向量接口。
- 前端知识库页面“重建向量”按钮。

## 新增数据库表

执行：

```sql
sql/ai_knowledge_embedding_schema_v1.sql
```

创建：

```text
ai_knowledge_embedding
```

关键字段：

```text
chunk_id              对应 ai_knowledge_chunk.id
document_id           对应 ai_knowledge_document.id
source_id             知识来源 ID
community_id          社区 ID
embedding_provider    HASH / OPENAI / QWEN / BGE 等
embedding_model       embedding 模型名
embedding_dimension   向量维度
embedding_vector      序列化后的向量
content_hash          chunk 内容 hash
status                ENABLED / DISABLED
```

## 新增接口

重建全部知识向量：

```http
POST /api/ai/knowledge/embeddings/rebuild
```

返回示例：

```json
{
  "rebuildBatchNo": "embedding-20260527171000",
  "scannedCount": 24,
  "embeddedCount": 24,
  "skippedCount": 0,
  "failedCount": 0,
  "provider": "HASH",
  "model": "hash-ngram-v1",
  "dimension": 128,
  "messages": []
}
```

## 默认 Embedding Provider

当前默认配置：

```yaml
smart-community:
  ai:
    embedding:
      provider: ${AI_EMBEDDING_PROVIDER:hash}
      hash:
        dimension: ${AI_HASH_EMBEDDING_DIMENSION:128}
```

`HASH` 是本地演示 Provider，用于先跑通工程链路。它不需要外部模型服务，也不会消耗 token。

面试时可以这样解释：

> 当前版本先实现了可替换 Embedding Provider 接口，并提供本地 Hash Embedding 用于演示和测试；生产环境可以把 provider 替换为 OpenAI、通义千问、BGE、bge-m3 等真实 embedding 模型。

## 核心代码定位

Embedding 抽象：

```text
ai-service/src/main/java/com/lsx/ai/knowledge/embedding/EmbeddingProvider.java
```

本地 Hash Embedding：

```text
ai-service/src/main/java/com/lsx/ai/knowledge/embedding/HashEmbeddingProvider.java
```

向量序列化：

```text
ai-service/src/main/java/com/lsx/ai/knowledge/embedding/VectorCodec.java
```

重建向量服务：

```text
ai-service/src/main/java/com/lsx/ai/knowledge/service/KnowledgeEmbeddingRebuildService.java
```

重建接口：

```text
ai-service/src/main/java/com/lsx/ai/knowledge/controller/KnowledgeEmbeddingController.java
```

前端按钮：

```text
D:/AIHbuilderProject/smart-community/admin/pages/admin/ai-knowledge.vue
```

## 验证步骤

1. 执行 SQL：

```sql
sql/ai_knowledge_embedding_schema_v1.sql
```

2. 重启 `AiServiceApplication`。

3. 在前端进入：

```text
AI 知识库 -> 重建向量
```

或直接请求：

```http
POST http://localhost:80/api/ai/knowledge/embeddings/rebuild
```

4. 查询数据库：

```sql
SELECT source_id, embedding_provider, embedding_model, embedding_dimension, status
FROM ai_knowledge_embedding
ORDER BY id DESC;
```

## 下一步

下一阶段要做：

```text
用户问题
 -> 生成 query embedding
 -> 读取 ai_knowledge_embedding
 -> 计算 cosine similarity
 -> 召回 topK chunks
 -> 和关键词分数融合
 -> 进入现有 RAG Prompt
```

也就是把当前的 `KeywordCommunityKnowledgeRetriever` 升级成 `HybridCommunityKnowledgeRetriever`。
