# 客服 RAG 回归测试集

这份测试集用于固定智能客服 RAG 的核心质量。每次改知识库、embedding 模型、召回阈值、Prompt 或部署配置后，都应该重新跑一遍。

## 测试目标

重点验证四件事：

```text
1. 可回答问题能命中正确 sourceId
2. 换一种说法的问题仍能通过 HYBRID 或 VECTOR 找到资料
3. 多问题混合提问能召回多条正确来源
4. 无资料或跨社区问题必须 cannotAnswer=true，不能乱答
```

## 前置条件

1. `ai-service` 已启动。
2. 网关可访问 `/api/ai/**`，默认测试地址为 `http://localhost:80`。
3. 客服知识库使用 JDBC：

```text
AI_CUSTOMER_KNOWLEDGE_STORE=jdbc
```

4. 已同步或导入基础知识，包括：

```text
PROCESS_REPAIR_001
FAQ_REPAIR_LEAK_001
POLICY_FEE_001
POLICY_VISITOR_001
PROCESS_COMPLAINT_001
NOTICE_WATER_001
```

5. 已导入一条电动车充电测试知识，并重建向量。

推荐内容：

```text
标题：电动车集中充电与飞线充电管理规定
关键词：电动车,集中充电,飞线充电,楼道充电,电池入户,消防安全
内容：为保障小区消防安全，电动车应停放在指定非机动车区域，并使用小区集中充电桩充电。严禁在楼道、单元门厅、地下车库疏散通道内停放电动车或私拉电线充电。严禁将电动车电池带入室内、楼道或电梯内充电。发现飞线充电、占用消防通道或电池入户充电的，物业将先提醒整改；拒不整改或存在明显安全隐患的，将联系社区网格员或消防部门协助处理。居民如发现相关隐患，可在业主端提交投诉建议，也可联系物业前台登记。
```

这条手工知识的 `sourceId` 通常是 `manual:*`，不同数据库会生成不同后缀。

## 字段解释

| 字段 | 含义 | 通过标准 |
| --- | --- | --- |
| `sourceId` | 知识来源 ID，例如 `NOTICE_WATER_001` | 应命中预期来源 |
| `cannotAnswer` | 是否无法回答 | 无资料问题必须为 `true` |
| `retrievalMode` | 检索方式：`HYBRID` / `VECTOR` / `KEYWORD` | 换说法问题应优先看到 `HYBRID` 或 `VECTOR` |
| `keywordScore` | 关键词命中分数 | 用于解释关键词贡献 |
| `vectorScore` | 向量语义命中分数 | 用于解释 embedding 贡献 |
| `citations` | 回答引用的 sourceId 列表 | 应与 sources 对得上 |

## 通过规则

```text
可回答问题：
  cannotAnswer=false
  sources 或 citations 包含预期 sourceId

换说法问题：
  cannotAnswer=false
  sources 包含预期 sourceId
  retrievalMode 最好是 HYBRID 或 VECTOR

多问题混合：
  cannotAnswer=false
  sources/citations 覆盖主要问题的来源

社区隔离：
  不能返回其他社区专属资料
  如果没有当前社区资料，应 cannotAnswer=true

明确无资料：
  cannotAnswer=true
  不应给出确定性业务答案
```

## 测试用例

| 编号 | 类型 | 社区ID | 测试问题 | 预期 cannotAnswer | 预期 sourceId | 预期 retrievalMode |
| --- | --- | --- | --- | --- | --- | --- |
| RAG-001 | 可回答 | 1 | 3栋本周六会停水吗？ | false | `NOTICE_WATER_001` | `HYBRID` 或 `KEYWORD` |
| RAG-002 | 可回答 | 1 | 厨房漏水怎么报修？ | false | `FAQ_REPAIR_LEAK_001` 或 `PROCESS_REPAIR_001` | `HYBRID` |
| RAG-003 | 可回答 | 1 | 物业费欠费怎么缴？ | false | `POLICY_FEE_001` | `HYBRID` 或 `KEYWORD` |
| RAG-004 | 可回答 | 1 | 亲戚来小区要怎么登记？ | false | `POLICY_VISITOR_001` | `HYBRID` 或 `KEYWORD` |
| RAG-005 | 可回答 | 1 | 噪音扰民投诉多久受理？ | false | `PROCESS_COMPLAINT_001` | `HYBRID` 或 `KEYWORD` |
| RAG-006 | 可回答 | 1 | 电动车可以在楼道充电吗？ | false | `manual:*` 电动车规定 | `HYBRID` |
| RAG-007 | 换说法 | 1 | 我能把小电驴电池拿回家充一晚上吗？ | false | `manual:*` 电动车规定 | `HYBRID` 或 `VECTOR` |
| RAG-008 | 换说法 | 1 | 家里突然没水，是不是周末有维修？ | false | `NOTICE_WATER_001` | `HYBRID` 或 `VECTOR` |
| RAG-009 | 换说法 | 1 | 水管爆了应该先怎么办？ | false | `PROCESS_REPAIR_001` 或 `FAQ_REPAIR_LEAK_001` | `HYBRID` 或 `VECTOR` |
| RAG-010 | 换说法 | 1 | 费用账单看不懂找谁核对？ | false | `POLICY_FEE_001` | `HYBRID` 或 `VECTOR` |
| RAG-011 | 换说法 | 1 | 外地亲戚过来住两天，门岗怎么放行？ | false | `POLICY_VISITOR_001` | `HYBRID` 或 `VECTOR` |
| RAG-012 | 多问题 | 1 | 3栋停水、厨房漏水、物业费欠费分别怎么办？ | false | `NOTICE_WATER_001`, `FAQ_REPAIR_LEAK_001`, `POLICY_FEE_001` | `HYBRID` 优先 |
| RAG-013 | 多问题 | 1 | 电梯困人、外来访客、账单疑问分别找谁？ | false | `PROCESS_REPAIR_001`, `POLICY_VISITOR_001`, `POLICY_FEE_001` | `HYBRID` 优先 |
| RAG-014 | 换说法 | 1 | 电动车飞线充电被发现会怎么处理？ | false | `manual:*` 电动车规定 | `HYBRID` 或 `VECTOR` |
| RAG-015 | 社区隔离 | 999 | 3栋本周六停水吗？ | true | 不应命中 `NOTICE_WATER_001` | 无要求 |
| RAG-016 | 社区隔离 | 2 | 3栋本周六停水吗？ | true | 如果停水通知只属于社区1，不应命中 `NOTICE_WATER_001` | 无要求 |
| RAG-017 | 明确无资料 | 1 | 小区游泳池几点开放？ | true | 无 | 无要求 |
| RAG-018 | 明确无资料 | 1 | 物业主任手机号是多少？ | true | 无 | 无要求 |
| RAG-019 | 明确无资料 | 1 | 快递柜密码是多少？ | true | 无 | 无要求 |
| RAG-020 | 明确无资料 | 1 | 车位价格下个月会不会涨？ | true | 无 | 无要求 |

## 手工记录模板

复制下面表格，每次回归测试后填实际结果：

| 编号 | 实际 cannotAnswer | 实际 sourceId/citations | 实际 retrievalMode | 是否通过 | 备注 |
| --- | --- | --- | --- | --- | --- |
| RAG-001 |  |  |  |  |  |
| RAG-002 |  |  |  |  |  |
| RAG-003 |  |  |  |  |  |
| RAG-004 |  |  |  |  |  |
| RAG-005 |  |  |  |  |  |
| RAG-006 |  |  |  |  |  |
| RAG-007 |  |  |  |  |  |
| RAG-008 |  |  |  |  |  |
| RAG-009 |  |  |  |  |  |
| RAG-010 |  |  |  |  |  |
| RAG-011 |  |  |  |  |  |
| RAG-012 |  |  |  |  |  |
| RAG-013 |  |  |  |  |  |
| RAG-014 |  |  |  |  |  |
| RAG-015 |  |  |  |  |  |
| RAG-016 |  |  |  |  |  |
| RAG-017 |  |  |  |  |  |
| RAG-018 |  |  |  |  |  |
| RAG-019 |  |  |  |  |  |
| RAG-020 |  |  |  |  |  |

## 常见异常判断

| 现象 | 可能原因 | 处理建议 |
| --- | --- | --- |
| 该回答的问题 `sources` 为空 | 知识未同步、状态禁用、社区ID不匹配 | 查 `ai_knowledge_document` 和 `ai_knowledge_chunk` |
| 换说法问题只出现 `KEYWORD` 或找不到 | 向量未重建或 provider/model 不一致 | 重建向量，查 `ai_knowledge_embedding` |
| 无资料问题返回弱相关 source | 向量阈值偏松 | 提高 vector similarity 阈值或加强拒答规则 |
| 社区隔离失败 | `community_id` 过滤异常或知识被设为通用 | 检查知识的 `community_id` 和 visibility |
| citations 和 sources 不一致 | LLM 输出或 normalizer 校正问题 | 优先以 normalized `citations` 和 `sources` 为准排查 |

## 免费自动执行方式

如果不能使用 IDEA `.http` 运行按钮，可以直接用 PowerShell 脚本跑完整测试集：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-rag-regression.ps1
```

默认配置：

```text
baseUrl = http://localhost:80
topK = 5
```

如果你直接访问 `ai-service:8090`，使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-rag-regression.ps1 -BaseUrl http://localhost:8090
```

如果你不想每次都重建向量，使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-rag-regression.ps1 -SkipRebuild
```

脚本会逐条输出：

```text
PASS/FAIL
cannotAnswer
sourceIds
retrievalMode
失败原因
```

最后会输出汇总：

```text
Summary: passed=20 failed=0 total=20
```

## HTTP 请求模板

请求模板见：

```text
docs/test-data/rag-customer-service-regression.http
```
