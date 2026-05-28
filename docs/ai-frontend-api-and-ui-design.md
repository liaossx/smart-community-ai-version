# 智能社区 AI 前端接口与页面设计文档

本文档面向前端开发，说明当前 AI 改造版本已经可对接的接口、字段含义、页面设计建议和业务闭环思路。

当前 AI 能力分为三块：

```text
1. 工单自动分派助手：报修内容分析、推荐班组、紧急程度、安全提示。
2. 智能社区客服助手：社区公告、物业制度、维修流程 RAG 问答。
3. 后台运营 AI 助手：从业务数据生成周报、风险提醒、居民诉求总结。
```

## 全局约定

### 服务地址

开发联调时可以直接访问：

```text
workorder-service: 走原有后端网关或 workorder-service 地址
ai-service: http://localhost:8090
```

当前 gateway-service 已增加 `ai-service` 路由：

```text
/api/ai/** -> http://localhost:8090
```

所以前端推荐统一走网关域名访问 AI 接口，例如：

```text
http://localhost:{gatewayPort}/api/ai/community/customer-service/ask
http://localhost:{gatewayPort}/api/ai/operations/weekly-report/from-db
```

如果 `ai-service` 部署地址不是 `localhost:8090`，在启动 gateway-service 时配置：

```text
AI_SERVICE_GATEWAY_URI=http://实际AI服务地址:端口
```

### AI 展示原则

前端不要把 AI 结果当成最终事实，建议统一按下面规则展示：

```text
manualReviewNeeded = true：显示“需要人工复核”醒目标识。
confidence >= 85：显示“高可信”。
65 <= confidence < 85：显示“建议人工确认”。
confidence < 65：显示“低可信，仅供参考”。
provider：显示在调试信息或详情中，方便判断来自规则还是大模型。
```

推荐文案：

```text
AI 建议，仅供物业人员辅助决策，最终处理结果以人工确认为准。
```

## 一、工单自动分派助手

### 使用位置

建议放在：

```text
后台管理端 -> 报修详情页
后台管理端 -> 工单详情页
```

页面目标：

```text
管理员打开报修单
 -> 点击“AI 分析”
 -> 查看分类、紧急程度、推荐班组、位置、安全提示
 -> 人工确认
 -> 创建或指派工单
```

### 接口 1：生成 AI 分析

```http
POST /api/workorder/ai/repair/{repairId}/analyze
```

路径参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| repairId | Long | 是 | 报修单 ID |

请求体：

```text
无
```

返回示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 8,
    "repairId": 201011,
    "workOrderId": null,
    "category": "WATER",
    "priority": 3,
    "urgencyLevel": "HIGH",
    "riskLevel": "MEDIUM",
    "recommendedTeam": "水暖维修组",
    "suggestedAction": "建议先联系住户确认漏水范围，提醒关闭就近阀门，并安排水暖维修组上门处理。",
    "summary": "住户反馈疑似漏水、排水或管道问题。提取位置：3栋2单元1801厨房水槽下方。",
    "extractedLocation": "3栋2单元1801厨房水槽下方",
    "suggestedResponseMinutes": 30,
    "safetyTips": "[\"提醒住户先关闭就近阀门。\",\"移开附近电器、贵重物品和易受潮物品。\"]",
    "matchedKeywords": "漏水,水管,水槽",
    "confidence": 88,
    "manualReviewNeeded": false,
    "provider": "RULE",
    "providerVersion": "rule-v1.1",
    "latest": true,
    "createTime": "2026-05-25T21:38:58"
  }
}
```

### 接口 2：读取最新 AI 分析

```http
GET /api/workorder/ai/repair/{repairId}/latest
```

用途：

```text
进入报修详情页时自动读取。
如果已有分析结果，直接展示。
如果没有 data，可提示管理员点击“AI 分析”。
```

### 接口 3：读取 AI 分析历史

```http
GET /api/workorder/ai/repair/{repairId}/history
```

用途：

```text
用于审计和对比多次分析结果。
前端可以放在“历史记录”抽屉或弹窗中。
```

### 字段说明

| 字段 | 说明 | 前端展示建议 |
| --- | --- | --- |
| category | AI 分类 | 显示为标签，如 WATER=水管 |
| priority | 优先级 | 1 普通，2 较急，3 紧急，4 高危特急 |
| urgencyLevel | 紧急程度 | LOW / MEDIUM / HIGH / CRITICAL |
| riskLevel | 风险等级 | LOW / MEDIUM / HIGH |
| recommendedTeam | 推荐班组 | 显示在“推荐处理”区域 |
| suggestedAction | 建议动作 | 作为主要 AI 建议 |
| summary | 摘要 | 放在卡片顶部 |
| extractedLocation | 提取位置 | 可和报修地址对比 |
| suggestedResponseMinutes | 建议响应时间 | 显示“建议 30 分钟内响应” |
| safetyTips | 安全提示 | 当前是 JSON 字符串，前端可解析成数组展示 |
| matchedKeywords | 命中关键词 | 展示为小标签 |
| confidence | 置信度 | 按全局规则显示 |
| manualReviewNeeded | 是否需要人工复核 | true 时高亮 |
| provider | 提供方 | RULE / SPRING_AI |

### 页面设计建议

报修详情页建议分成三块：

```text
左侧：报修信息
右侧上方：AI 分析卡片
右侧下方：人工确认区
```

AI 分析卡片内容：

```text
摘要
分类标签
紧急程度
风险等级
推荐班组
建议响应时间
建议处理动作
安全提示
命中关键词
置信度
provider/providerVersion
```

按钮建议：

```text
生成 AI 分析
重新分析
采用建议
人工修改
创建工单 / 指派班组
```

当前后端已经完成“推荐”，但“采用建议后自动创建/指派工单”的专用接口还没有单独封装。前端第一版可以先展示 AI 建议，由管理员手动走现有创建/指派流程；后续再新增“采用 AI 建议”接口。

## 二、智能社区客服助手 RAG

### 使用位置

建议放在：

```text
业主端 -> 在线客服
后台端 -> 客服辅助台
后台端 -> 知识库管理
```

页面目标：

```text
居民输入问题
 -> 系统检索公告/制度/FAQ
 -> 大模型基于检索资料回答
 -> 返回答案、引用来源、后续动作
 -> 无法回答时转人工
```

### 接口 1：客服问答

```http
POST http://localhost:8090/api/ai/community/customer-service/ask
Content-Type: application/json
```

请求参数：

```json
{
  "communityId": 1,
  "question": "小区电动车在哪里充电？",
  "topK": 3
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| communityId | Long | 否 | 社区 ID；传入后优先检索该社区资料 |
| question | String | 是 | 居民问题 |
| topK | Integer | 否 | 检索前几条资料，建议默认 3 |

返回示例：

```json
{
  "answer": "小区电动车可以在增设的集中充电桩充电。请注意，严禁将电动车进楼入户充电，请自觉遵守规定。",
  "followUpActions": [
    "如需了解集中充电桩的具体位置，请联系物业客服。",
    "请遵守小区电动车充电规定，确保安全。"
  ],
  "citations": [
    "sys_notice:28"
  ],
  "cannotAnswer": false,
  "confidence": 65,
  "provider": "SPRING_AI_RAG",
  "providerVersion": "rag-v1",
  "model": "deepseek-v4-flash",
  "sources": [
    {
      "sourceId": "sys_notice:28",
      "sourceType": "COMMUNITY_NOTICE",
      "title": "关于规范电动车充电的通知",
      "excerpt": "严禁电动车进楼入户充电！小区内已增设集中充电桩，请各位业主为了安全自觉遵守规定。",
      "score": 9
    }
  ]
}
```

无法回答示例：

```json
{
  "answer": "暂未在社区知识库中找到可以回答该问题的资料，请联系物业客服人工确认。",
  "followUpActions": [],
  "citations": [],
  "cannotAnswer": true,
  "confidence": 30,
  "provider": "RAG_RETRIEVAL",
  "providerVersion": "rag-v1",
  "model": "deepseek-v4-flash",
  "sources": []
}
```

### 字段说明

| 字段 | 说明 | 前端展示建议 |
| --- | --- | --- |
| answer | AI 答案 | 主内容 |
| followUpActions | 后续动作 | 展示为行动清单 |
| citations | 引用 ID | 可折叠显示 |
| cannotAnswer | 是否无法回答 | true 时显示“转人工”按钮 |
| confidence | 置信度 | 按全局规则展示 |
| provider | 提供方 | SPRING_AI_RAG / RAG_RETRIEVAL |
| sources | 检索来源 | 展示标题、片段、score |

### 页面设计建议

客服页建议采用聊天式布局：

```text
顶部：社区选择、问题输入框
中间：问答消息流
右侧或底部：引用来源 sources
底部：转人工、复制答案、评价答案
```

当 `cannotAnswer=true`：

```text
不展示“AI 已确认”类文案。
显示“转人工客服”主按钮。
可以把 question 和 sources 一起带给人工客服。
```

当 `sources` 不为空：

```text
答案下方展示“参考资料”。
点击来源可打开公告/制度/FAQ 详情。
```

## 三、知识库管理接口

这部分主要给后台管理员使用，用于维护 FAQ、物业制度、维修流程等长期知识。

### 同步公告进知识库

```http
POST http://localhost:8090/api/ai/knowledge/sync/notices
```

用途：

```text
管理员发布 sys_notice 后，把公告同步进 RAG 知识库。
如果已开启定时同步，前端可以不提供这个按钮。
```

### 新增知识文档

```http
POST http://localhost:8090/api/ai/knowledge/documents
Content-Type: application/json
```

请求示例：

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

### 修改知识文档

```http
PUT http://localhost:8090/api/ai/knowledge/documents/{id}
```

### 查询单条知识文档

```http
GET http://localhost:8090/api/ai/knowledge/documents/{id}
```

### 分页查询知识文档

```http
GET http://localhost:8090/api/ai/knowledge/documents?keyword=漏水&status=ENABLED&pageNum=1&pageSize=10
```

可选查询参数：

| 参数 | 说明 |
| --- | --- |
| keyword | 标题/内容关键词 |
| sourceType | FAQ / PROPERTY_POLICY / COMMUNITY_NOTICE 等 |
| status | ENABLED / DISABLED |
| communityId | 社区 ID |
| pageNum | 页码 |
| pageSize | 每页数量 |

### 禁用知识文档

```http
POST http://localhost:8090/api/ai/knowledge/documents/{id}/disable
```

页面建议：

```text
知识列表
 -> 新增知识
 -> 编辑知识
 -> 禁用知识
 -> 测试提问
```

知识管理页最好提供一个“测试提问”区，管理员新增 FAQ 后，可以直接调用客服 RAG 接口验证是否能命中。

## 四、后台运营 AI 助手

### 使用位置

建议放在：

```text
后台首页 -> AI 运营概览
后台运营中心 -> 周报生成页
社区管理端 -> 社区风险看板
```

页面目标：

```text
管理员选择社区和日期范围
 -> 系统从业务表聚合 sourceData
 -> AI 生成运营周报
 -> 管理员查看风险、诉求、建议动作
 -> 后续生成待办、公告或工单
```

### 接口 1：从数据库生成周报

```http
GET http://localhost:8090/api/ai/operations/weekly-report/from-db?communityId=1&startDate=2026-06-01&endDate=2026-06-07
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| communityId | Long | 否 | 社区 ID；不传则统计全部社区 |
| startDate | String | 是 | 开始日期，格式 YYYY-MM-DD |
| endDate | String | 是 | 结束日期，格式 YYYY-MM-DD |

返回示例：

```json
{
  "source": "MYSQL",
  "sourceData": {
    "communityId": 1,
    "communityName": "压测演示社区",
    "startDate": "2026-06-01",
    "endDate": "2026-06-07",
    "repairTotal": 18,
    "repairPending": 15,
    "repairCompleted": 3,
    "urgentRepairCount": 18,
    "complaintTotal": 10,
    "complaintPending": 8,
    "visitorTotal": 18,
    "feeUnpaidCount": 8,
    "noticePublishedCount": 4,
    "topRepairCategories": ["电路：4单", "水管：4单", "公共设施：4单"],
    "residentAppeals": ["安全隐患：1件", "高空抛物：1件"],
    "recentRiskEvents": ["报修#201101：电路，厨房插座漏电...（优先级3）"]
  },
  "report": {
    "reportTitle": "压测演示社区运营周报（2026-06-01至2026-06-07）",
    "executiveSummary": "本周社区运营风险明显升高，紧急维修和待处理投诉较多，建议安排人工复核。",
    "weeklyHighlights": [
      "本周报修总量较高，其中紧急报修占比较大。",
      "投诉待处理量达到 8 件，需要重点跟进。"
    ],
    "riskAlerts": [
      {
        "riskType": "OPERATIONS_BACKLOG",
        "riskLevel": "HIGH",
        "description": "存在紧急报修和投诉积压。",
        "suggestedAction": "安排管理员复核待处理事项，明确责任人和完成时间。"
      }
    ],
    "residentAppealSummary": "居民诉求集中在安全隐患、停车管理、电梯问题等方面。",
    "recommendedActions": [
      "优先处理高危维修和消防相关风险。",
      "对待处理投诉建立跟进清单。",
      "必要时发布公告说明处理进度。"
    ],
    "manualReviewNeeded": true,
    "confidence": 95,
    "provider": "SPRING_AI_OPERATIONS",
    "providerVersion": "operations-ai-v1",
    "model": "deepseek-v4-flash"
  }
}
```

### 接口 2：手动传入聚合数据生成周报

```http
POST http://localhost:8090/api/ai/operations/weekly-report
Content-Type: application/json
```

用途：

```text
如果未来由其他服务先聚合数据，可以直接把聚合结果传给 ai-service。
当前前端优先使用 from-db 接口。
```

请求体示例：

```json
{
  "communityId": 1,
  "communityName": "压测演示社区",
  "startDate": "2026-06-01",
  "endDate": "2026-06-07",
  "repairTotal": 18,
  "repairPending": 15,
  "repairCompleted": 3,
  "urgentRepairCount": 18,
  "complaintTotal": 10,
  "complaintPending": 8,
  "visitorTotal": 18,
  "feeUnpaidCount": 8,
  "noticePublishedCount": 4,
  "topRepairCategories": ["电路：4单", "水管：4单"],
  "residentAppeals": ["安全隐患：1件", "高空抛物：1件"],
  "recentRiskEvents": ["报修#201101：厨房插座漏电"]
}
```

### sourceData 字段说明

| 字段 | 说明 |
| --- | --- |
| repairTotal | 周期内报修总数 |
| repairPending | 周期内待处理/处理中报修数 |
| repairCompleted | 周期内已完成报修数 |
| urgentRepairCount | 紧急报修数，来自工单优先级和风险关键词 |
| complaintTotal | 周期内投诉总数 |
| complaintPending | 周期内待处理/处理中投诉数 |
| visitorTotal | 周期内访客数 |
| feeUnpaidCount | 当前未缴费账单存量，不按周过滤 |
| noticePublishedCount | 周期内发布公告数 |
| topRepairCategories | 高频报修类型 |
| residentAppeals | 居民诉求分类 |
| recentRiskEvents | 近期风险事件 |

### 页面设计建议

运营周报页建议布局：

```text
顶部筛选区：
社区选择、日期范围、生成周报按钮

指标概览区：
报修总数、紧急报修、投诉待处理、访客数、欠费数、公告数

AI 周报区：
运营摘要、周亮点、风险提醒、居民诉求总结、建议动作

数据来源区：
sourceData 折叠面板，给管理员核对 AI 是基于哪些数据生成的
```

风险提醒展示：

```text
LOW：普通提示
MEDIUM：黄色提示
HIGH：红色风险
CRITICAL：强提醒，需要立即处理
```

当 `manualReviewNeeded=true`：

```text
显示“需要人工复核”。
建议提供“生成待办”按钮。
建议提供“复制周报”按钮。
```

## 五、前端页面路由建议

可以先设计这几个页面：

```text
/admin/workorder/repair/:repairId
报修详情页，增加 AI 工单分析卡片。

/admin/ai/customer-service
后台客服辅助页，支持输入问题、查看答案和来源。

/admin/ai/knowledge
知识库管理页，维护 FAQ / 制度 / 流程。

/admin/ai/operations
运营 AI 周报页，选择日期生成周报。
```

业主端可以后续增加：

```text
/owner/customer-service
居民 AI 客服入口。
```

## 六、闭环设计建议

### 工单闭环

当前：

```text
AI 分析 -> 管理员查看 -> 人工确认
```

后续增强：

```text
AI 分析 -> 管理员采用建议 -> 自动生成工单 -> 指派班组 -> 处理完成 -> 复盘 AI 是否准确
```

建议新增字段或接口：

```text
aiAnalysisId
adopted: 是否采纳
manualCategory
manualTeam
manualPriority
adoptRemark
```

### 客服闭环

当前：

```text
居民提问 -> RAG 回答 -> 展示来源
```

后续增强：

```text
居民提问 -> AI 回答 -> 满意/不满意 -> 转人工 -> 沉淀新 FAQ
```

建议新增能力：

```text
客服问答记录表
满意度评价
无法回答问题列表
一键转为 FAQ
```

### 运营闭环

当前：

```text
业务表聚合 -> AI 生成周报 -> 管理员查看
```

后续增强：

```text
AI 周报 -> 生成待办 -> 指派负责人 -> 跟踪完成 -> 下周复盘
```

建议新增能力：

```text
AI 周报保存表
风险待办表
周报导出
周报发布给管理层
```

## 七、前端联调检查清单

联调时按这个顺序检查：

```text
1. ai-service 是否启动在 8090。
2. DeepSeek 环境变量是否配置。
3. RAG 是否使用 jdbc 知识库。
4. 工单分析接口是否能返回 RULE 或 SPRING_AI。
5. 客服问答是否能返回 citations 和 sources。
6. 运营周报 sourceData 是否有真实数据。
7. manualReviewNeeded 和 confidence 是否按风险变化。
8. 前端是否处理 cannotAnswer=true。
9. 前端是否处理 provider=RULE/RAG_RETRIEVAL 等兜底情况。
10. 所有 AI 结果是否保留人工确认入口。
```

## 八、建议优先级

第一版优先做：

```text
1. 报修详情页 AI 分析卡片。
2. 后台运营 AI 周报页。
3. 后台客服 RAG 测试页。
```

第二版再做：

```text
1. 业主端 AI 客服聊天页。
2. 知识库管理页。
3. AI 结果采纳和反馈闭环。
```

这样前端改造会更稳：先让管理员看到 AI 能力，再逐步开放给居民端。
