# 向量 RAG 第二步：混合检索

这一步把社区客服 RAG 从“只有关键词检索”升级成了：

```text
关键词检索 + 向量检索 + 分数融合
```

也就是更接近面试里能讲的完整链路：

```text
知识入库
 -> chunk 切分
 -> embedding 入库
 -> 用户问题生成 query embedding
 -> keyword + vector 混合召回
 -> 拼 Prompt
 -> Spring AI 调用大模型
 -> 规则校正 citations / confidence
 -> 返回答案和来源
```

## 这一步改了什么

### 1. 新增向量检索器

```text
ai-service/src/main/java/com/lsx/ai/customer/knowledge/VectorCommunityKnowledgeRetriever.java
```

职责：

```text
读取 ai_knowledge_embedding
 -> 解码 embedding_vector
 -> 计算 query 和 chunk 的 cosine similarity
 -> 召回 topK 结果
```

当前实现说明：

- 继续使用 MySQL，不额外引入向量数据库
- 相似度计算在 Java 里完成
- 这是“先跑通核心链路”的工程版本

### 2. 新增混合检索器

```text
ai-service/src/main/java/com/lsx/ai/customer/knowledge/HybridCommunityKnowledgeRetriever.java
```

职责：

```text
先跑 KeywordCommunityKnowledgeRetriever
再跑 VectorCommunityKnowledgeRetriever
按 chunk 合并结果
融合 keywordScore 和 vectorScore
输出最终 retrieval score
```

检索模式有三种：

```text
KEYWORD  只有关键词命中
VECTOR   只有向量命中
HYBRID   关键词和向量同时命中
```

### 3. RAG 主流程切到混合检索

```text
ai-service/src/main/java/com/lsx/ai/customer/service/RagCustomerServiceAssistant.java
```

原来依赖：

```text
KeywordCommunityKnowledgeRetriever
```

现在依赖：

```text
HybridCommunityKnowledgeRetriever
```

这说明现在客服问答的检索阶段已经不是纯关键词版了。

### 4. sources 增加检索解释字段

```text
ai-service/src/main/java/com/lsx/ai/customer/dto/RagSource.java
```

新增：

```text
keywordScore
vectorScore
retrievalMode
```

这样你调接口时就能直接看到某条来源是怎么命中的。

## 你现在要做的手动步骤

1. 重启 `AiServiceApplication`
2. 在前端“AI 知识库”页面点击一次“重建向量”

或者直接调接口：

```http
POST http://localhost:80/api/ai/knowledge/embeddings/rebuild
```

3. 看到类似返回，说明向量已经重建成功：

```json
{
  "rebuildBatchNo": "embedding-20260527174500",
  "scannedCount": 24,
  "embeddedCount": 24,
  "failedCount": 0
}
```

4. 再去测试客服问答接口：

```http
POST http://localhost:80/api/ai/community/customer-service/ask
Content-Type: application/json

{
  "communityId": 2,
  "question": "小区电动车在哪里充电？",
  "topK": 3
}
```

## 你应该重点观察什么

除了 `answer` 之外，重点看 `sources`：

```json
{
  "sourceId": "sys_notice:28",
  "score": 27,
  "keywordScore": 18,
  "vectorScore": 63,
  "retrievalMode": "HYBRID"
}
```

你可以这样理解：

- `keywordScore`：文本关键词匹配得分
- `vectorScore`：embedding 相似度得分
- `score`：最终融合后的检索得分
- `retrievalMode`：本次来源是关键词、向量，还是混合命中

## 面试时可以怎么讲

你可以这样说：

> 第一版 RAG 先用 MySQL + 关键词检索把业务闭环跑通，验证知识入库、引用来源和回答链路。第二版在不额外引入向量数据库的前提下，为 chunk 建立 embedding，查询时对用户问题生成 query embedding，在 Java 层计算 cosine similarity，再与关键词检索做 hybrid retrieval。这样既保留了规则可解释性，也补上了语义召回能力。

## 下一步还能怎么拔高

如果后面你还想继续升级，这条线最自然的下一步是：

```text
1. 把 Hash Embedding Provider 替换成真实 embedding 模型
2. 增加 rerank
3. 把 embedding 存储从 MySQL 升级到 pgvector / Milvus
4. 在前端展示检索解释和引用详情
5. 增加反馈闭环，统计哪些问题回答差
```
