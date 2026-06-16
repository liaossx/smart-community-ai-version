# AI 知识体系 - smart-community-ai-version

本文档从知识结构角度梳理项目中的全部 AI 相关概念和设计决策，方便按模块深入学习。

---

## 1. 总体架构：为什么 AI 是一个独立服务

```
用户请求 --> Gateway /api/ai/** --> ai-service :8090 --> DeepSeek API
                                       |
                                     MySQL
```

设计理由：
- AI 调用是慢操作（10-30 秒），独立部署避免拖垮业务服务
- 统一的 ChatClient / EmbeddingModel 配置，避免各业务服务重复配置
- 独立的可观测性日志表，方便追踪每次 AI 调用的成本和效果

---

## 2. RAG（检索增强生成）- 项目最核心的 AI 模式

### 2.1 概念

RAG = Retrieval-Augmented Generation：在调用大模型之前，先从知识库中检索相关资料，拼接进 Prompt 再让模型回答。

```
用户问: "小区维修电话多少"
  |
  v
1) 检索: 从知识库搜到 -> [资料1: 物业维修热线 010-1234]
  |
  v
2) 拼装 Prompt: system(固定) + 居民问题 + 社区资料
  |
  v
3) 大模型生成: "小区物业维修热线是 010-1234，工作时间 8:00-20:00"
```

### 2.2 为什么需要 RAG

| 问题 | 不用 RAG | 用 RAG |
|---|---|---|
| 模型不知道本项目信息 | 胡编乱造 | 引用真实资料 |
| 资料更新 | 需要重新微调模型 | 更新数据库即可 |
| 回答可追溯 | 无法验证来源 | 每条回答带 citation |

### 2.3 本项目 RAG 的四步流程

```
[1) 混合检索]  Keyword + Vector 双路，加权合并
     |
     v  List<RetrievedKnowledgeDocument>
[2) Prompt拼装]  system(固定) + user(问题 + 检索资料)
     |
     v  String (prompt)
[3) 模型生成]  ChatClient.call() -> DeepSeek
     |
     v  CustomerServiceAnswerResponse
[4) 输出修正]  Normalizer: 校验citation, 补全字段, 兜底文案
```

### 2.4 混合检索知识点

关键字检索：基于分词匹配，精确但僵硬。
- 居民问"漏水" -> 精确匹配到标题/关键字含"漏水"的资料
- 居民问"天花板往下淌水" -> 可能匹配不到

向量检索：基于语义相似度，模糊但覆盖面广。
- 将文本转为向量（embedding），计算余弦相似度
- "天花板往下淌水" 的向量接近 "漏水"，所以能搜到

混合评分公式：
```
finalScore = max(关键字得分, 向量得分 x 0.35) + (两边同时命中 ? +5 : 0)
```
向量仅起辅助作用（权重 0.35），关键字命中权重更高。

### 2.5 Embedding 是什么

Embedding 是把文本变成一串数字（向量），语义相近的文本向量也相近。

```
"漏水"           -> [0.82, -0.31, 0.55, ...]  (1536维)
"天花板往下淌水"   -> [0.79, -0.28, 0.51, ...]  <- 非常接近
"小区停车费"      -> [-0.12, 0.67, -0.44, ...] <- 完全不接近
```

本项目支持两种 Provider：
- hash: 本地 Hash 函数，128维，演示/开发用，不产生费用
- openai: 真实 Embedding API，1536维

### 2.6 "无资料拒绝"策略

```java
if (documents.isEmpty()) {
    return noKnowledgeAnswer();
}
```
理由：省钱（避免无意义API调用）+ 安全（防止模型胡编）

---

## 3. Prompt Engineering - 系统中的提示词设计

### 3.1 System Prompt 的五个要素

```java
private static final String SYSTEM_PROMPT = String.join("\n",
    "你是智能社区客服助手。",                    // 1) 角色设定
    "你只能根据提供的社区资料回答问题...",         // 2) 能力边界
    "如果资料不足以...cannotAnswer 必须为 true",   // 3) 兜底规则
    "citations 只能填写资料中的 sourceId。",      // 4) 枚举约束
    "返回结构化对象，字段包括 answer、..."         // 5) 输出格式
);
```

1. 角色设定 - 让模型进入正确的行为模式
2. 能力边界 - 明确什么不能做
3. 兜底规则 - 资料不足时的行为
4. 枚举约束 - 限制可选值
5. 输出格式 - 告诉模型返回什么字段

### 3.2 为什么 System Prompt 要 static final

与前面讨论的上下文缓存直接相关：

```
DeepSeek KV Cache:
  system: [你是智能社区客服助手。你只能根据...]  <- 200 token, 永远不变 -> 全命中
  user:   [居民问题：{漏水了}]                    <- 每次不同 -> 未命中

首次请求: system 按 $0.14/1M 计费
后续请求: system 按 $0.0028/1M 计费 (便宜 50 倍)
```

### 3.3 User Prompt 的"上下文块"模式

```
居民问题：{question}
社区资料：
[1] sourceId: NOTICE-2024-001
title: 小区停水通知
content: 因管道维修，3号楼将于6月5日8:00-18:00停水...
请只根据社区资料回答
```

编号供模型引用，Normalizer 校验 sourceId 真实性。

---

## 4. Normalizer 模式 - 为什么不能直接信任模型输出

### 4.1 问题

大模型输出是非确定性的，可能返回空字段、编造 sourceId、confidence=999、非法枚举值。

### 4.2 解决：每个链路都有一个 Normalizer

```
Assistant.answer()
  |
  +- chatClient.call() -> 模型原始输出
  |
  +- normalizer.normalize() -> 修正后输出
       +- 空字段补默认值
       +- 数字 clamp 到合法范围
       +- citation 白名单过滤
       +- 枚举值校验
       +- 模糊词/高危词二次判断
```

### 4.3 Normalizer 的原则

- 不修改模型的实质内容
- 只做防御性修正（填坑、校验、兜底）
- 和 Assistant 解耦，方便独立测试

---

## 5. Fallback 策略 - AI 不可用时的应对

```java
try {
    response = chatClient.call();         // 尝试 AI
    result = normalizer.normalize(response);
} catch (Exception e) {
    result = normalizer.normalize(ruleFallback());  // 降级到规则
}
```

各链路的 Fallback 行为：

| 链路 | AI 正常 | AI 失败 |
|---|---|---|
| 客服 RAG | 模型回答 + citation | 返回检索到的原文列表 |
| 工单分析 | 模型分类 | 抛出异常，上游处理 |
| 运营洞察 | 模型生成洞察卡片 | 基于 if/else 规则生成保底卡片 |
| 运营周报 | 模型生成完整周报 | 模板化保底周报 |

关键设计：AI 失败时页面不能空白。

---

## 6. 可观测性 - 追踪每次 AI 调用

```java
AiCallLogEntry.start("CUSTOMER_SERVICE_RAG")   // bizType
    .bizKey("communityId=123")                  // 业务关联
    .requestSummary("漏水怎么办")                 // 请求摘要
    .provider("SPRING_AI", "rag-v1", "deepseek-v4-flash")
    .status("SUCCESS")
    .confidence(85)
    .latencyMs()                                // 自动计算
```

用途：成本追踪、质量监控、问题排查。

---

## 7. 四条 AI 调用链路对比

|             | 客服 RAG           | 工单分析           | 运营洞察              | 运营周报              |
|-------------|-------------------|-------------------|----------------------|----------------------|
| 有检索？     | 是（关键字+向量）    | 否                 | 否（可自动聚合DB）      | 否（可自动聚合DB）       |
| Prompt结构  | system + 资料 + 问题 | system + 报修信息   | system + 运营指标      | system + 运营指标       |
| 模型输出     | answer/citations  | category/priority  | insightCards/actions | reportTitle/alerts    |
| AI失败时     | 返回检索原文        | 抛异常              | 规则生成保底卡片        | 模板化保底周报          |
| 模型       | deepseek-v4-flash | deepseek-v4-flash  | deepseek-v4-flash    | deepseek-v4-flash     |

---

## 8. 学习路线建议（按复杂度从低到高）

**第1步：工单分析（最简单）**
文件：SpringAiWorkOrderAnalyzer.java
概念：Prompt拼装 -> 模型调用 -> Normalizer修正
重点：system prompt 的枚举约束怎么写

**第2步：运营周报**
文件：SpringAiOperationsReportAssistant.java
概念：同上 + Fallback 策略
重点：理解为什么 AI 失败时不能用空白页

**第3步：运营洞察**
文件：SpringAiOperationsInsightsAssistant.java
概念：同上 + 双模式（手动指标 / 自动聚合）
重点：理解"数据聚合"和"AI 分析"是两个独立步骤

**第4步：RAG 客服（最复杂）**
文件：RagCustomerServiceAssistant.java
      -> HybridCommunityKnowledgeRetriever.java
      -> VectorCommunityKnowledgeRetriever.java
      -> EmbeddingProvider.java
概念：检索 -> 增强 -> 生成 -> 修正 全链路
重点：混合检索排序算法、向量余弦相似度、citation 白名单校验

**第5步：可观测性（横切）**
文件：AiCallLogEntry.java -> AiCallLogService.java
概念：每个链路的埋点、成本追踪
重点：bizType / status / confidence 的设计
