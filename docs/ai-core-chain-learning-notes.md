# AI 核心链路学习笔记

本文档用于阅读两条核心 AI 业务链路：

```text
1. 智能社区客服助手：社区公告、物业制度、维修流程 RAG 问答。
2. 后台运营 AI 助手：根据社区数据生成周报、风险提醒、居民诉求总结。
```

## 一、智能社区客服助手 RAG

### 业务链路

```text
前端提交居民问题
 -> CustomerServiceAiController 接收请求
 -> RagCustomerServiceAssistant 编排 RAG 流程
 -> KeywordCommunityKnowledgeRetriever 从知识库检索相关资料
 -> CommunityKnowledgeRepository 读取知识来源
 -> 拼装 question + context Prompt
 -> Spring AI 调用 DeepSeek
 -> CustomerServiceAnswerNormalizer 校正答案、引用、置信度
 -> 返回 answer + citations + sources
```

如果没有检索到资料：

```text
不调用大模型
 -> 直接 cannotAnswer=true
 -> 提示转人工
```

如果检索到了资料，但大模型调用失败：

```text
返回 RAG_RETRIEVAL fallback
 -> 告诉前端“查到了相关资料，但模型暂不可用”
 -> sources 仍然返回，便于客服人工查看
```

### 推荐阅读顺序

1. `ai-service/src/main/java/com/lsx/ai/customer/controller/CustomerServiceAiController.java`

   RAG 问答入口。

   你重点看：

   ```java
   @PostMapping("/ask")
   public CustomerServiceAnswerResponse ask(...)
   ```

2. `ai-service/src/main/java/com/lsx/ai/customer/service/RagCustomerServiceAssistant.java`

   RAG 主流程。

   你重点看：

   ```java
   retriever.retrieve(...)
   chatClient.prompt()
   .system(SYSTEM_PROMPT)
   .user(...)
   .call()
   .entity(CustomerServiceAnswerResponse.class)
   normalizer.normalize(...)
   ```

   这就是：

   ```text
   检索 -> 上下文封装 -> 调模型 -> 结构化输出 -> 规则校正
   ```

3. `ai-service/src/main/java/com/lsx/ai/customer/knowledge/KeywordCommunityKnowledgeRetriever.java`

   当前版本的检索器。

   你重点看：

   ```java
   retrieve(...)
   score(...)
   filterLowRelevance(...)
   ```

   当前是关键词检索。以后接向量数据库，主要就是替换这一层。

4. `ai-service/src/main/java/com/lsx/ai/customer/knowledge/JdbcCommunityKnowledgeRepository.java`

   MySQL 知识库读取。

   它读取：

   ```text
   ai_knowledge_document
   ai_knowledge_chunk
   ```

   然后转换成：

   ```text
   KnowledgeDocument
   ```

5. `ai-service/src/main/java/com/lsx/ai/customer/service/CustomerServiceAnswerNormalizer.java`

   RAG 输出校正器。

   你重点看：

   ```java
   normalizeCitations(...)
   normalizeConfidence(...)
   minConfidenceFromRetrievalScore(...)
   ```

   这里解决三个企业级问题：

   ```text
   防止模型编造引用
   防止模型置信度乱给
   防止空答案直接返回给居民
   ```

### 这条链路你要学到什么

RAG 不是“直接问大模型”，而是：

```text
先查资料
再把资料放进 Prompt
要求模型只能基于资料回答
最后后端校验引用和输出结构
```

你现在项目里的 RAG 版本是：

```text
MySQL 知识库 + 关键词检索 + Spring AI + 结构化输出 + 引用校验
```

以后企业级增强会变成：

```text
MySQL 知识库
 -> 文档切片
 -> embedding
 -> 向量数据库
 -> 混合检索
 -> rerank
 -> answer with citations
 -> 反馈评估
```

## 二、后台运营 AI 助手

### 业务链路

```text
前端选择社区和日期范围
 -> OperationsAiController 接收请求
 -> OperationsMetricsAggregationService 从 MySQL 汇总 sourceData
 -> SpringAiOperationsReportAssistant 拼装运营 Prompt
 -> Spring AI 调用 DeepSeek
 -> OperationsReportNormalizer 校正结构、风险、置信度
 -> 返回 sourceData + report
```

如果大模型调用失败：

```text
SpringAiOperationsReportAssistant 捕获异常
 -> ruleFallback 根据 sourceData 生成保底周报
 -> provider=RULE_OPERATIONS
```

### 推荐阅读顺序

1. `ai-service/src/main/java/com/lsx/ai/operations/controller/OperationsAiController.java`

   运营周报入口。

   你重点看：

   ```java
   @GetMapping("/weekly-report/from-db")
   generateWeeklyReportFromDb(...)
   ```

   这里体现：

   ```text
   先聚合 sourceData
   再生成 report
   ```

2. `ai-service/src/main/java/com/lsx/ai/operations/service/OperationsMetricsAggregationService.java`

   SQL 聚合层。

   它读取：

   ```text
   biz_repair
   biz_work_order
   sys_complaint
   sys_visitor
   sys_fee
   sys_notice
   sys_community
   ```

   然后生成：

   ```text
   OperationsReportRequest
   ```

   你重点看：

   ```java
   aggregateWeeklyReportData(...)
   countUrgentRepairs(...)
   queryTopRepairCategories(...)
   queryResidentAppeals(...)
   queryRecentRiskEvents(...)
   ```

3. `ai-service/src/main/java/com/lsx/ai/operations/service/SpringAiOperationsReportAssistant.java`

   运营周报模型调用层。

   你重点看：

   ```java
   SYSTEM_PROMPT
   buildOperationData(...)
   chatClient.prompt()
   .entity(OperationsReportResponse.class)
   ruleFallback(...)
   ```

   这里体现：

   ```text
   业务指标 -> Prompt 上下文 -> 大模型结构化输出
   ```

4. `ai-service/src/main/java/com/lsx/ai/operations/service/OperationsReportNormalizer.java`

   运营周报输出校正层。

   你重点看：

   ```java
   normalizeRisks(...)
   manualReviewNeeded 判断
   calibrateConfidence(...)
   ```

   这里体现：

   ```text
   模型输出不能直接信
   风险等级要校正
   置信度要按业务规则兜底
   ```

### 这条链路你要学到什么

运营 AI 不是 RAG，它更像：

```text
业务数据分析助手
```

核心区别：

```text
RAG：先检索知识，再回答问题。
运营助手：先聚合数据，再总结分析。
```

运营助手的关键是：

```text
sourceData 必须可信
Prompt 必须要求模型不能编造
风险判断不能完全交给模型
输出必须经过 Java 规则校正
```

你现在项目里的运营助手是：

```text
MySQL 业务表
 -> SQL 指标聚合
 -> 风险关键词规则
 -> Spring AI 生成周报
 -> normalizer 校正风险和 confidence
 -> 前端展示 sourceData + report
```

企业级后续增强会是：

```text
异步生成周报
周报保存和版本管理
风险待办流转
模型失败缓存上一次结果
Provider 健康检查
运营指标口径配置
报表导出
AI 输出质量评估
```

## 三、两条链路的共同模式

你可以把这两条 AI 功能统一理解成：

```text
输入
 -> 后端准备可信上下文
 -> Prompt 封装
 -> 大模型结构化输出
 -> Java 规则校正
 -> 前端人工确认/展示
```

区别只是“可信上下文”的来源不同：

```text
客服 RAG：上下文来自知识库检索。
运营周报：上下文来自业务 SQL 聚合。
工单助手：上下文来自用户报修描述和规则 Provider。
```

## 四、什么时候继续做企业级优化

建议等前端完成第一版之后再继续深挖。

判断标准：

```text
运营周报页面能生成并展示 report。
客服 RAG 页面能展示 answer 和 sources。
报修详情页能展示 AI 分析卡片。
前端 timeout、loading、错误提示都处理好。
```

做到这里以后，再进入企业级优化会更有意义，因为你能看见真实使用问题：

```text
哪些问题回答不准
哪些知识检索不到
哪些接口太慢
哪些 AI 结果需要人工修改
哪些场景需要缓存/异步
```

下一阶段推荐优化顺序：

```text
1. AI 调用异步化，避免前端长时间等待。
2. 保存周报和问答记录，便于复盘。
3. RAG 接入 embedding 和向量数据库。
4. 增加 AI Provider 健康检查和切换。
5. 增加人工反馈，用真实业务数据优化 Prompt 和规则。
```
