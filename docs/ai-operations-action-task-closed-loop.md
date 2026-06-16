# AI 运营洞察执行闭环

这一步把 `AI 洞察` 的 `actionItems` 从“只展示建议”升级成“生成督办任务并跟踪状态”。

## 目标

形成下面这条闭环：

```text
业务表聚合指标
 -> AI 洞察生成风险和 actionItems
 -> actionItems 落库成运营督办任务
 -> 责任人更新状态
 -> 回写反馈结果
 -> 形成已闭环记录
```

## 先执行 SQL

执行：

```sql
sql/ai_operations_action_task_schema_v1.sql
```

创建表：

```text
ai_operations_action_task
```

核心字段：

```text
task_batch_no         同一次生成的任务批次号
community_id          社区ID
overall_risk_level    洞察整体风险等级
priority              P0/P1/P2/P3
owner_role            责任角色
task_title            督办任务标题
task_reason           任务原因
deadline_text         截止描述
status                TODO/IN_PROGRESS/BLOCKED/DONE/CANCELLED
feedback_result       执行反馈
feedback_by           反馈人
closed_loop           是否已闭环
```

## 接口 1：从洞察生成督办任务

```http
POST /api/ai/operations/action-tasks/from-insights
Content-Type: application/json
```

请求体：

```json
{
  "communityId": 1,
  "startDate": "2026-06-01",
  "endDate": "2026-06-07"
}
```

说明：

```text
后端会先从业务表聚合 sourceData
再生成 AI 洞察
再把洞察里的 actionItems 落库成督办任务
```

返回里会包含：

```text
taskBatchNo
sourceData
insights
createdCount
tasks
```

## 接口 2：查询督办任务列表

```http
GET /api/ai/operations/action-tasks?communityId=1&pageNum=1&pageSize=20
```

可选筛选：

```text
status
taskBatchNo
communityId
```

## 接口 3：查询单条督办任务

```http
GET /api/ai/operations/action-tasks/{id}
```

## 接口 4：更新任务状态和反馈

```http
PUT /api/ai/operations/action-tasks/{id}/status
Content-Type: application/json
```

请求体示例：

```json
{
  "status": "IN_PROGRESS",
  "feedbackResult": "已联系工程维修负责人，今晚安排排查地下车库积水风险。",
  "feedbackBy": "张主管"
}
```

完成闭环示例：

```json
{
  "status": "DONE",
  "feedbackResult": "已完成排水、张贴警示牌，并回访业主确认现场恢复。",
  "feedbackBy": "李经理"
}
```

状态说明：

```text
TODO         待处理
IN_PROGRESS  处理中
BLOCKED      阻塞中
DONE         已完成
CANCELLED    已取消
```

当状态是 `DONE` 或 `CANCELLED` 时，后端会自动把：

```text
closed_loop = true
feedback_time = now()
```

## 建议的手工演示顺序

1. 执行 `sql/ai_operations_weekly_report_test_data.sql`
2. 执行 `sql/ai_operations_action_task_schema_v1.sql`
3. 调用 `POST /api/ai/operations/action-tasks/from-insights`
4. 看返回中的 `tasks`
5. 调用 `GET /api/ai/operations/action-tasks`
6. 选一条任务，调用 `PUT /api/ai/operations/action-tasks/{id}/status`
7. 再次查询该任务，确认 `status`、`feedbackResult`、`closedLoop`

## 面试时怎么讲

可以这样讲：

```text
第一版 AI 洞察只能输出风险和建议动作。
我后来把 actionItems 真正落库成运营督办任务，并补了状态流转和反馈结果。
这样 AI 不只是“会分析”，而是能把分析结果推进到执行闭环里。
```
