# AI Service New Capabilities

`ai-service` now contains three AI capability slices:

1. Work order dispatch assistant
2. Community customer service RAG assistant
3. Operations weekly report assistant

The service still runs independently on Java 17 and defaults to port `8090`.

## Community Customer Service RAG

Endpoint:

```http
POST http://localhost:8090/api/ai/community/customer-service/ask
Content-Type: application/json
```

Example request:

```json
{
  "communityId": 1,
  "question": "3栋周六是不是要停水？我需要提前做什么？",
  "topK": 3
}
```

The current version supports two knowledge stores:

```text
static: in-process demo knowledge, default
jdbc: MySQL tables ai_knowledge_document / ai_knowledge_chunk
```

It is a RAG learning slice:

```text
question
 -> retrieve relevant knowledge documents
 -> build context
 -> call LLM with context
 -> return answer plus citations and sources
```

To switch the RAG assistant to MySQL knowledge:

```powershell
$env:AI_CUSTOMER_KNOWLEDGE_STORE="jdbc"
$env:AI_CUSTOMER_KNOWLEDGE_JDBC_URL="jdbc:mysql://localhost:3306/smart_community?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:AI_CUSTOMER_KNOWLEDGE_JDBC_USERNAME="root"
$env:AI_CUSTOMER_KNOWLEDGE_JDBC_PASSWORD="your-password"
```

If you already use `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, and `MYSQL_PASSWORD`, the default config can reuse them.

### Sync Notices Into RAG Knowledge

After an admin publishes notices in `sys_notice`, sync them into the AI knowledge tables:

```http
POST http://localhost:8090/api/ai/knowledge/sync/notices
```

The sync reads notices where:

```text
publish_status = PUBLISHED
deleted = 0
expire_time is null or expire_time > now()
```

For each notice, it upserts:

```text
ai_knowledge_document
ai_knowledge_chunk
ai_knowledge_sync_log
```

It also disables AI knowledge for notices that are deleted, offline, unpublished, or expired.

Expected response shape:

```json
{
  "syncBatchNo": "notice-20260526161200",
  "scannedCount": 3,
  "syncedCount": 3,
  "disabledCount": 0,
  "failedCount": 0,
  "messages": []
}
```

After syncing, ask the RAG endpoint again. If `AI_CUSTOMER_KNOWLEDGE_STORE=jdbc`, the answer should use knowledge from MySQL.

To enable scheduled notice sync:

```powershell
$env:AI_NOTICE_KNOWLEDGE_SYNC_ENABLED="true"
$env:AI_NOTICE_KNOWLEDGE_SYNC_INITIAL_DELAY_MS="30000"
$env:AI_NOTICE_KNOWLEDGE_SYNC_FIXED_DELAY_MS="300000"
```

Default behavior:

```text
enabled: false
initial delay: 30 seconds
fixed delay: 5 minutes
```

When enabled, `ai-service` periodically performs the same work as:

```http
POST /api/ai/knowledge/sync/notices
```

### Manage Long-Term Knowledge

Use these APIs for knowledge that does not come from `sys_notice`, such as property policies, repair processes, parking rules, decoration rules, and FAQ entries.

Create a knowledge document:

```http
POST http://localhost:8090/api/ai/knowledge/documents
Content-Type: application/json
```

```json
{
  "sourceType": "FAQ",
  "sourceId": "FAQ_REPAIR_LEAK_001",
  "communityId": null,
  "title": "厨房漏水如何报修",
  "content": "居民发现厨房漏水后，应先关闭就近水阀，移开电器和贵重物品，并在业主端提交报修。紧急漏水可同时电话联系物业值班人员。",
  "keywords": ["厨房", "漏水", "报修", "水阀", "物业值班"],
  "status": "ENABLED",
  "visibility": "RESIDENT"
}
```

Update a knowledge document:

```http
PUT http://localhost:8090/api/ai/knowledge/documents/{id}
```

Get one document:

```http
GET http://localhost:8090/api/ai/knowledge/documents/{id}
```

List documents:

```http
GET http://localhost:8090/api/ai/knowledge/documents?keyword=漏水&status=ENABLED&pageNum=1&pageSize=10
```

Disable a document:

```http
POST http://localhost:8090/api/ai/knowledge/documents/{id}/disable
```

Create/update automatically maintains the first `ai_knowledge_chunk`, so the RAG assistant can retrieve the knowledge immediately when `AI_CUSTOMER_KNOWLEDGE_STORE=jdbc`.

Important code:

- `ai-service/src/main/java/com/lsx/ai/customer/knowledge/StaticCommunityKnowledgeRepository.java`
- `ai-service/src/main/java/com/lsx/ai/customer/knowledge/JdbcCommunityKnowledgeRepository.java`
- `ai-service/src/main/java/com/lsx/ai/customer/knowledge/KeywordCommunityKnowledgeRetriever.java`
- `ai-service/src/main/java/com/lsx/ai/customer/service/RagCustomerServiceAssistant.java`
- `ai-service/src/main/java/com/lsx/ai/customer/service/CustomerServiceAnswerNormalizer.java`
- `ai-service/src/main/java/com/lsx/ai/knowledge/service/NoticeKnowledgeSyncService.java`
- `ai-service/src/main/java/com/lsx/ai/knowledge/controller/KnowledgeSyncController.java`
- `ai-service/src/main/java/com/lsx/ai/knowledge/task/NoticeKnowledgeSyncTask.java`
- `ai-service/src/main/java/com/lsx/ai/knowledge/service/KnowledgeDocumentAdminService.java`
- `ai-service/src/main/java/com/lsx/ai/knowledge/controller/KnowledgeDocumentAdminController.java`
- `ai-service/src/main/java/com/lsx/ai/customer/controller/CustomerServiceAiController.java`

Key response fields:

```text
answer: final resident-facing answer
citations: sourceId list used by the answer
sources: retrieved knowledge snippets
cannotAnswer: true when knowledge is insufficient
provider: SPRING_AI_RAG or RAG_RETRIEVAL fallback
providerVersion: rag-v1
model: configured model name
```

Next upgrade path:

```text
Static knowledge
 -> sys_notice / policy table
 -> embedding model
 -> vector store
 -> hybrid retrieval
```

## Operations Weekly Report Assistant

Endpoint:

```http
POST http://localhost:8090/api/ai/operations/weekly-report
Content-Type: application/json
```

Example request:

```json
{
  "communityId": 1,
  "communityName": "阳光花园",
  "startDate": "2026-05-18",
  "endDate": "2026-05-24",
  "repairTotal": 38,
  "repairPending": 6,
  "repairCompleted": 29,
  "urgentRepairCount": 2,
  "complaintTotal": 12,
  "complaintPending": 5,
  "visitorTotal": 186,
  "feeUnpaidCount": 23,
  "noticePublishedCount": 4,
  "topRepairCategories": ["漏水", "电路", "电梯"],
  "residentAppeals": ["停车位紧张", "夜间噪音", "电梯等待时间长"],
  "recentRiskEvents": ["3栋出现两次漏水报修", "地下车库照明故障"]
}
```

This endpoint expects already-aggregated business data. Business services should own data queries; `ai-service` owns summarization.

New learning endpoint for direct MySQL aggregation:

```http
GET http://localhost:8090/api/ai/operations/weekly-report/from-db?communityId=1&startDate=2026-05-18&endDate=2026-05-24
```

`communityId` is optional. If it is omitted, the report aggregates all communities.

This endpoint reads the current business tables directly:

```text
biz_repair       -> repairTotal / repairPending / repairCompleted / topRepairCategories
biz_work_order   -> urgentRepairCount and high-risk repair signals
sys_complaint    -> complaintTotal / complaintPending / residentAppeals / complaint risk events
sys_visitor      -> visitorTotal
sys_fee          -> feeUnpaidCount
sys_notice       -> noticePublishedCount
sys_community    -> communityName
```

Response shape:

```json
{
  "source": "MYSQL",
  "sourceData": {
    "communityId": 1,
    "communityName": "幸福家园",
    "startDate": "2026-05-18",
    "endDate": "2026-05-24",
    "repairTotal": 3,
    "repairPending": 2,
    "repairCompleted": 1,
    "urgentRepairCount": 1,
    "complaintTotal": 2,
    "complaintPending": 1,
    "visitorTotal": 6,
    "feeUnpaidCount": 5,
    "noticePublishedCount": 2,
    "topRepairCategories": ["水管：2单", "电路：1单"],
    "residentAppeals": ["噪音扰民：1件", "设施故障：1件"],
    "recentRiskEvents": ["报修#201011：水管，厨房漏水...（优先级3）"]
  },
  "report": {
    "reportTitle": "幸福家园运营周报（2026-05-18至2026-05-24）",
    "executiveSummary": "...",
    "weeklyHighlights": [],
    "riskAlerts": [],
    "residentAppealSummary": "...",
    "recommendedActions": [],
    "manualReviewNeeded": true,
    "confidence": 90,
    "provider": "SPRING_AI_OPERATIONS",
    "providerVersion": "operations-ai-v1",
    "model": "deepseek-v4-flash"
  }
}
```

The important learning chain is:

```text
business tables
 -> SQL aggregation and keyword risk rules
 -> OperationsReportRequest
 -> Spring AI Prompt
 -> structured OperationsReportResponse
 -> normalizer safety and confidence correction
```

Confidence is not trusted directly from the model. For `SPRING_AI_OPERATIONS`:

```text
no operational signal: minimum confidence 80
normal SQL metrics available: minimum confidence 85
urgent repair / complaint backlog / risk events: minimum confidence 90
```

`RULE_OPERATIONS` fallback keeps its own lower confidence because it did not complete the model generation path.

Operations data source config defaults to the same MySQL connection used by JDBC RAG. Override only if operations uses another database:

```powershell
$env:AI_OPERATIONS_JDBC_URL="jdbc:mysql://localhost:3306/smart_community?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:AI_OPERATIONS_JDBC_USERNAME="root"
$env:AI_OPERATIONS_JDBC_PASSWORD="your-password"
```

Important code:

- `ai-service/src/main/java/com/lsx/ai/operations/service/SpringAiOperationsReportAssistant.java`
- `ai-service/src/main/java/com/lsx/ai/operations/service/OperationsMetricsAggregationService.java`
- `ai-service/src/main/java/com/lsx/ai/operations/service/OperationsReportNormalizer.java`
- `ai-service/src/main/java/com/lsx/ai/operations/controller/OperationsAiController.java`
- `ai-service/src/main/java/com/lsx/ai/operations/dto/OperationsReportFromDbResponse.java`

Key response fields:

```text
reportTitle
executiveSummary
weeklyHighlights
riskAlerts
residentAppealSummary
recommendedActions
manualReviewNeeded
provider: SPRING_AI_OPERATIONS or RULE_OPERATIONS fallback
providerVersion: operations-ai-v1
model
```

## Verification

Use JDK 17:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f ai-service\pom.xml test
```

Current verification result:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```
