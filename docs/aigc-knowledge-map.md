# AIGC 智能客服模块 - 核心链路与名词手册

---

## 一、核心链路（一次问答的完整旅程）

```
① 用户发问题（HTTP POST）
      │  question: "小区维修电话多少"
      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  CustomerServiceAiController                                            │
│  职责：接收请求，转发给 Assistant。不处理 AI 逻辑。                        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  RagCustomerServiceAssistant.answer()                                   │
│  职责：编排 RAG 全流程。                                                 │
│                                                                         │
│  ② 混合检索                                                              │
│     └── HybridCommunityKnowledgeRetriever.retrieve()                    │
│           │                                                             │
│           ├── 关键字检索：KeywordCommunityKnowledgeRetriever            │
│           │     MySQL LIKE 匹配标题和内容，按命中关键词数量打分。         │
│           │                                                             │
│           └── 向量语义检索：VectorCommunityKnowledgeRetriever           │
│                 EmbeddingProvider.embed(question) → 向量                │
│                 → MySQL 库里存了每个知识文档的向量                        │
│                 → 算余弦相似度 → 过滤 < 0.18 的噪声 → 按分数排序          │
│                                                                         │
│          两路结果合并：max(关键字得分, 向量得分×0.35) + 双命中加 5 分     │
│                                                                         │
│  ③ 无资料拒绝                                                           │
│     检索结果为空 → 不调大模型 → 直接返回 "请联系物业客服人工确认"         │
│                                                                         │
│  ④ Prompt 拼装                                                          │
│     system: SYSTEM_PROMPT（固定常量，200 token）                         │
│     user: "居民问题：{question}\n社区资料：[1] title...content...\n..." │
│                                                                         │
│  ⑤ 调用大模型                                                            │
│     ChatClient.prompt().system(...).user(...).call()                    │
│     → HTTP POST https://api.deepseek.com/v1/chat/completions            │
│     → DeepSeek 思考 + 生成 → 返回结构化 JSON                            │
│                                                                         │
│  ⑥ Normalizer 输出校验                                                  │
│     CustomerServiceAnswerNormalizer.normalize()                         │
│     ├── citations 白名单过滤（模型编造的 sourceId 直接丢弃）              │
│     ├── confidence 限幅（0-100，cannotAnswer 时上限 50）                 │
│     ├── answer 为空补兜底文案                                           │
│     └── 补全 provider / model 等后端字段                                │
│                                                                         │
│  ⑦ 可观测性日志                                                          │
│     AiCallLogService.record()                                           │
│     记录 requestId、提问内容、检索到的 sourceId、回答内容、耗时、状态     │
│                                                                         │
│  ⑧ 模型不可用 → Fallback                                                │
│     大模型抛异常 → 返回检索到的原文列表 + "请根据引用资料联系人工客服"     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、关键名词速查表

### 架构层

| 名词 | 做什么的 | 对应代码 |
|---|---|---|
| **RAG** (检索增强生成) | 先查知识库 → 拼进 Prompt → 再让模型回答。不 RAG 的话模型不知道本社区信息只能编。 | `RagCustomerServiceAssistant` |
| **混合检索** (Hybrid Retrieval) | 关键字 + 向量两路都搜，加权合并。关键字精确但僵硬；向量模糊但覆盖面广。两路互补。 | `HybridCommunityKnowledgeRetriever` |
| **System Prompt** (系统提示词) | 写死在 `static final` 常量里的角色说明书。告诉模型你是谁、能做什么不能做什么、输出什么格式。利用 DeepSeek KV Cache，每次都是命中缓存。 | `RagCustomerServiceAssistant.SYSTEM_PROMPT` |
| **Normalizer** (输出校验器) | 模型的输出不能直接信任。Normalizer 做的事：citations 只保留真正检索到的 sourceId、confidence 做限幅、空字段补默认值。防御性修正。 | `CustomerServiceAnswerNormalizer` |
| **Fallback** (降级) | 大模型挂了不报错，返回检索到的原文列表。让前端客户至少知道查到了哪些资料。 | `RagCustomerServiceAssistant.retrievalFallback()` |

### 检索层

| 名词 | 做什么的 | 对应代码 |
|---|---|---|
| **关键字检索** (Keyword Retrieval) | 用 MySQL `LIKE` 匹配知识文档的标题和内容。按命中关键词数量打分。 | `KeywordCommunityKnowledgeRetriever` |
| **向量检索** (Vector Retrieval) | 把用户问题转成 Embedding 向量 → 跟知识库里每条文档的向量算余弦相似度 → 越接近 1 表示语义越相似。 | `VectorCommunityKnowledgeRetriever` |
| **Embedding** (嵌入) | 把一句话变成一串数字（向量）。语义相近的句子向量也相近。"天花板往下淌水" 的向量接近 "漏水"。 | `EmbeddingProvider` |
| **余弦相似度** (Cosine Similarity) | 衡量两个向量"方向"有多接近的指标。值在 -1 到 1 之间。项目里 < 0.18 的直接当噪声丢弃。 | `VectorCommunityKnowledgeRetriever` 里的 `cosineSimilarity` |
| **Top-K** | 检索返回几条最匹配的结果。默认 3，最多 5。 | `DEFAULT_TOP_K = 3, MAX_TOP_K = 5` |
| **检索评分公式** | `max(关键字分, 向量分×0.35) + 双命中加 5 分`。关键字权重高因为精确匹配置信度更高。向量辅助兜底。 | `HybridCommunityKnowledgeRetriever.mergeResults()` |

### 生成层

| 名词 | 做什么的 | 对应代码 |
|---|---|---|
| **ChatClient** | Spring AI 提供的大模型调用客户端。一行 `.call()` 就调 DeepSeek。内部是 HTTP POST 到 OpenAI 兼容接口。 | `ChatClient` |
| **上下文块** (Context Block) | 检索到的每条资料拼成带编号的文本块：`[1] sourceId=... title=... content=...`。编号让模型回答时可以写 citations 引用哪个资料。 | `KnowledgeDocument.toContextBlock()` |
| **Thinking Mode** | DeepSeek 的思考模式。模型先生成内部推理链（你不可见），再输出回答。这是为什么一次调用要 10-30 秒。 | DeepSeek V4 默认行为 |

### 安全层

| 名词 | 做什么的 | 对应代码 |
|---|---|---|
| **无资料拒绝** (No-Knowledge Reject) | 检索不到资料就不调大模型。省钱 + 防编造。 | `documents.isEmpty()` → `noKnowledgeAnswer()` |
| **citations 白名单** | 本次检索到的所有 sourceId 组成白名单。模型返回的 citations 不在白名单里就丢弃。防止模型编造引用。 | `normalizeCitations()` |
| **confidence 限幅** | 把模型输出的置信度 clamp 到 0-100。如果 cannotAnswer=true 上限压到 50。 | `clampConfidence()` |

---

## 三、面试回答模板（60 秒）

面试官问："你这个 AI 智能客服具体是怎么做的？"

回答：

> 我做的是 RAG 智能客服——不是直接调大模型，而是先检索、再增强、再生成。用户问一个问题，先走混合检索——关键字和向量两路都搜。关键字用 MySQL LIKE 匹配标题内容，向量用 Embedding 算余弦相似度。两路结果加权合并，关键字权重高、向量做语义兜底。
>
> 检索不到资料的话不调大模型——直接返回"请联系人工客服"，省钱也安全。搜到了就把资料拼进 Prompt——system 是固定的角色提示词，user 里塞居民问题和检索到的资料块。然后调 DeepSeek 生成回答。
>
> 模型返回后走 Normalizer 做安全校验——citations 必须在本轮检索结果的 sourceId 白名单里，不在的直接丢弃；confidence 做限幅；空字段补兜底文案。每次调用都记了可观测性日志——耗时、状态、检索到的资料。
