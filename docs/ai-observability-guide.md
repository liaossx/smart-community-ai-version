# AI 调用观测与审计日志

这一阶段的目标是让 AI 功能从“能调用、能返回”往“可排查、可运营、可评估”靠近。

## 为什么要做这一步

真实企业系统里，AI 调用不能只看前端返回结果，还需要知道：

- 哪个业务调用了 AI。
- 使用了哪个 provider、模型和 prompt/provider 版本。
- 调用成功、失败，还是走了兜底。
- 本次耗时多久。
- 最终置信度是多少。
- RAG 命中了哪些资料。
- 失败时错误是什么。

这些信息后面可以支持成本统计、失败排查、效果评估、模型切换对比和 prompt 版本回溯。

## 数据库表

先执行：

```sql
sql/ai_call_log_schema_v1.sql
```

创建表：

```text
ai_call_log
```

核心字段：

```text
request_id            本次 AI 调用唯一 ID
biz_type              CUSTOMER_SERVICE_RAG / OPERATIONS_REPORT / WORKORDER_ANALYZE
biz_key               业务主键，例如 repairId=201011、communityId=1
provider              SPRING_AI_RAG / SPRING_AI_OPERATIONS / SPRING_AI / RULE_OPERATIONS / RAG_RETRIEVAL
provider_version      当前能力版本，例如 rag-v1、operations-ai-v1
model                 模型名，例如 deepseek-v4-flash
status                SUCCESS / FALLBACK / FAILED
latency_ms            调用耗时
confidence            最终归一化后的置信度
request_summary       请求摘要
response_summary      响应摘要
retrieved_source_ids  RAG 命中的资料 sourceId
error_message         失败或兜底原因
```

## 后端接口

查询 AI 调用记录：

```http
GET /api/ai/observability/call-logs?pageNum=1&pageSize=20
```

按业务类型筛选：

```http
GET /api/ai/observability/call-logs?bizType=CUSTOMER_SERVICE_RAG&pageNum=1&pageSize=20
GET /api/ai/observability/call-logs?bizType=OPERATIONS_REPORT&pageNum=1&pageSize=20
GET /api/ai/observability/call-logs?bizType=WORKORDER_ANALYZE&pageNum=1&pageSize=20
```

按状态筛选：

```http
GET /api/ai/observability/call-logs?status=SUCCESS&pageNum=1&pageSize=20
GET /api/ai/observability/call-logs?status=FALLBACK&pageNum=1&pageSize=20
GET /api/ai/observability/call-logs?status=FAILED&pageNum=1&pageSize=20
```

## 已接入日志的链路

### 智能社区客服助手

入口：

```text
POST /api/ai/community/customer-service/ask
```

记录内容：

- 问题文本。
- communityId。
- RAG 命中的 sourceId。
- provider：`SPRING_AI_RAG` 或 `RAG_RETRIEVAL`。
- confidence。
- 大模型失败时记录 `FALLBACK`。

核心代码：

```text
ai-service/src/main/java/com/lsx/ai/customer/service/RagCustomerServiceAssistant.java
```

### 后台运营 AI 助手

入口：

```text
GET /api/ai/operations/weekly-report/from-db
POST /api/ai/operations/weekly-report
```

记录内容：

- 聚合后的 sourceData 摘要。
- provider：`SPRING_AI_OPERATIONS` 或 `RULE_OPERATIONS`。
- confidence。
- 大模型失败时记录 `FALLBACK`。

核心代码：

```text
ai-service/src/main/java/com/lsx/ai/operations/service/SpringAiOperationsReportAssistant.java
```

### 工单 AI 分析助手

入口：

```text
POST /api/ai/workorder/analyze
```

记录内容：

- repairId。
- faultType/faultDesc 摘要。
- provider：`SPRING_AI`。
- confidence。
- 大模型失败时记录 `FAILED`。

核心代码：

```text
ai-service/src/main/java/com/lsx/ai/workorder/service/SpringAiWorkOrderAnalyzer.java
```

## 配置

默认开启：

```yaml
smart-community:
  ai:
    observability:
      enabled: ${AI_OBSERVABILITY_ENABLED:true}
```

默认复用知识库 MySQL 连接。也可以单独配置：

```text
AI_OBSERVABILITY_JDBC_URL=jdbc:mysql://localhost:3306/smart_community?...
AI_OBSERVABILITY_JDBC_USERNAME=root
AI_OBSERVABILITY_JDBC_PASSWORD=你的密码
```

如果没有执行建表 SQL，业务接口不会失败，后端只会提示表不存在。执行 SQL 后重启 `ai-service` 即可开始记录。

## 验证顺序

1. 执行 `sql/ai_call_log_schema_v1.sql`。
2. 重启 `AiServiceApplication`。
3. 调一次 AI 客服、AI 运营周报或工单 AI 分析接口。
4. 查询：

```http
GET http://localhost:80/api/ai/observability/call-logs?pageNum=1&pageSize=20
```

5. 确认返回里有 `bizType`、`provider`、`status`、`latencyMs`、`confidence`。
