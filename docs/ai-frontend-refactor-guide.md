# AI 功能前端改造落地文档

本文档用于指导前端把后端已经完成的 AI 能力接入页面。它比 `ai-frontend-api-and-ui-design.md` 更偏代码落地：改请求封装、加 API 模块、加页面、处理超时、处理 AI 兜底状态。

当前后端已完成三类 AI 能力：

```text
1. 工单自动分派助手
2. 智能社区客服 RAG 助手
3. 后台运营 AI 周报助手
```

## 一、改造目标

前端最终要支持：

```text
后台运营页：选择日期 -> 生成 AI 周报 -> 查看风险和建议
报修详情页：点击 AI 分析 -> 查看推荐班组、紧急程度、安全提示
客服页：输入问题 -> AI 基于知识库回答 -> 展示引用来源
知识库页：维护 FAQ / 制度 / 流程，给 RAG 使用
```

第一阶段优先做：

```text
1. 后台运营 AI 周报页
2. 报修详情页 AI 分析卡片
3. 后台客服 RAG 测试页
```

第二阶段再做：

```text
1. 知识库管理页
2. 业主端 AI 客服页
3. AI 结果采纳、反馈、保存和导出
```

## 二、前端基础配置改造

### 1. 统一 AI 接口基地址

不要在页面里直接写死：

```text
http://localhost:8090
```

建议统一放到前端配置里。

开发环境推荐优先走 gateway：

```text
http://localhost
```

因为 gateway 已经配置：

```text
/api/ai/** -> http://localhost:8090
```

如果临时不启动 gateway，也可以直连：

```text
http://localhost:8090
```

建议配置示例：

```js
// config/env.js
export const API_BASE_URL = 'http://localhost'
export const AI_BASE_URL = 'http://localhost'
```

如果你暂时只想直连 ai-service：

```js
export const AI_BASE_URL = 'http://localhost:8090'
```

### 2. AI 接口 timeout 单独调大

AI 接口会调用 DeepSeek，大模型返回可能需要 10-30 秒。前端默认 timeout 如果是 10 秒，很容易出现：

```text
request:fail timeout
```

建议普通接口和 AI 接口分开设置：

```js
const DEFAULT_TIMEOUT = 10000
const AI_TIMEOUT = 60000
```

uni-app 请求封装示例：

```js
export function request(options) {
  const isAiRequest = options.url && options.url.includes('/api/ai/')

  return new Promise((resolve, reject) => {
    uni.request({
      url: options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: options.header || {},
      timeout: options.timeout || (isAiRequest ? 60000 : 10000),
      success: (res) => {
        resolve(res.data)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}
```

更推荐封装一个 AI 专用请求：

```js
export function aiRequest(options) {
  return request({
    ...options,
    timeout: 60000
  })
}
```

### 3. 统一错误文案

前端需要区分三种错误：

```text
CORS：浏览器跨域拦截，一般说明没走 gateway 或后端没开 CORS。
timeout：请求已发出，但 AI 返回慢，前端提前放弃。
Connection reset：后端调用大模型 Provider 网络波动，重试后可能成功。
```

建议文案：

```js
function getAiErrorMessage(error) {
  const message = error && (error.errMsg || error.message || '')

  if (message.includes('timeout')) {
    return 'AI 生成耗时较长，请稍后重试，或扩大查询范围后异步生成。'
  }
  if (message.includes('CORS') || message.includes('Access-Control-Allow-Origin')) {
    return '接口跨域受限，请检查是否通过网关访问。'
  }
  if (message.includes('Connection reset')) {
    return '大模型服务网络波动，请稍后重试。'
  }
  return 'AI 服务暂时不可用，请稍后重试。'
}
```

## 三、API 模块设计

建议新增一个前端文件：

```text
src/api/ai.js
```

如果你的项目不是 `src` 结构，就放到现有 `api` 目录下。

### 1. 运营 AI API

```js
import { aiRequest } from '@/utils/request'
import { AI_BASE_URL } from '@/config/env'

export function generateOperationsReport(params) {
  return aiRequest({
    url: `${AI_BASE_URL}/api/ai/operations/weekly-report/from-db`,
    method: 'GET',
    data: {
      communityId: params.communityId,
      startDate: params.startDate,
      endDate: params.endDate
    }
  })
}
```

参数：

```js
{
  communityId: 1,
  startDate: '2026-06-01',
  endDate: '2026-06-07'
}
```

### 2. 客服 RAG API

```js
export function askCustomerService(data) {
  return aiRequest({
    url: `${AI_BASE_URL}/api/ai/community/customer-service/ask`,
    method: 'POST',
    data: {
      communityId: data.communityId,
      question: data.question,
      topK: data.topK || 3
    }
  })
}
```

### 3. 知识库 API

```js
export function listKnowledgeDocuments(params) {
  return aiRequest({
    url: `${AI_BASE_URL}/api/ai/knowledge/documents`,
    method: 'GET',
    data: params
  })
}

export function createKnowledgeDocument(data) {
  return aiRequest({
    url: `${AI_BASE_URL}/api/ai/knowledge/documents`,
    method: 'POST',
    data
  })
}

export function updateKnowledgeDocument(id, data) {
  return aiRequest({
    url: `${AI_BASE_URL}/api/ai/knowledge/documents/${id}`,
    method: 'PUT',
    data
  })
}

export function disableKnowledgeDocument(id) {
  return aiRequest({
    url: `${AI_BASE_URL}/api/ai/knowledge/documents/${id}/disable`,
    method: 'POST'
  })
}

export function syncNoticesToKnowledge() {
  return aiRequest({
    url: `${AI_BASE_URL}/api/ai/knowledge/sync/notices`,
    method: 'POST'
  })
}
```

### 4. 工单 AI API

工单 AI 接口属于 `workorder-service`，通常走原有后端基地址。

```js
import { request } from '@/utils/request'
import { API_BASE_URL } from '@/config/env'

export function analyzeRepairByAi(repairId) {
  return request({
    url: `${API_BASE_URL}/api/workorder/ai/repair/${repairId}/analyze`,
    method: 'POST',
    timeout: 60000
  })
}

export function getLatestRepairAiAnalysis(repairId) {
  return request({
    url: `${API_BASE_URL}/api/workorder/ai/repair/${repairId}/latest`,
    method: 'GET'
  })
}

export function getRepairAiAnalysisHistory(repairId) {
  return request({
    url: `${API_BASE_URL}/api/workorder/ai/repair/${repairId}/history`,
    method: 'GET'
  })
}
```

## 四、页面一：后台运营 AI 周报页

建议页面路径：

```text
/admin/ai/operations
```

如果是 uni-app，可新增：

```text
pages/admin/ai/operations.vue
```

### 页面功能

```text
选择社区
选择开始日期
选择结束日期
点击“生成 AI 周报”
展示 sourceData 指标
展示 report 周报内容
展示风险提醒
展示建议动作
```

### 页面状态

```js
data() {
  return {
    query: {
      communityId: 1,
      startDate: '2026-06-01',
      endDate: '2026-06-07'
    },
    loading: false,
    sourceData: null,
    report: null,
    errorMessage: ''
  }
}
```

### 生成周报方法

```js
async generateReport() {
  this.loading = true
  this.errorMessage = ''

  try {
    const res = await generateOperationsReport(this.query)
    this.sourceData = res.sourceData
    this.report = res.report
  } catch (error) {
    this.errorMessage = getAiErrorMessage(error)
  } finally {
    this.loading = false
  }
}
```

### 展示结构

建议页面分 4 块：

```text
1. 查询条件区
2. 指标概览区
3. AI 周报区
4. 原始数据区
```

指标概览区展示：

```text
报修总数 repairTotal
待处理报修 repairPending
紧急报修 urgentRepairCount
投诉总数 complaintTotal
待处理投诉 complaintPending
访客数 visitorTotal
欠费数 feeUnpaidCount
公告数 noticePublishedCount
```

AI 周报区展示：

```text
reportTitle
executiveSummary
weeklyHighlights
riskAlerts
residentAppealSummary
recommendedActions
manualReviewNeeded
confidence
provider / model
```

### 风险颜色规则

```js
function getRiskClass(level) {
  const value = String(level || '').toUpperCase()
  if (value === 'CRITICAL') return 'risk-critical'
  if (value === 'HIGH') return 'risk-high'
  if (value === 'MEDIUM') return 'risk-medium'
  return 'risk-low'
}
```

建议颜色：

```text
CRITICAL：红色强提醒
HIGH：红色
MEDIUM：黄色
LOW：灰色/蓝色
```

### 测试数据按钮

开发阶段建议在页面上放 3 个快捷按钮：

```text
低量周：2026-05-18 ~ 2026-05-24
常规周：2026-05-25 ~ 2026-05-31
高风险周：2026-06-01 ~ 2026-06-07
```

点击后自动填日期并生成周报。

## 五、页面二：报修详情页 AI 分析卡片

建议接入位置：

```text
后台报修详情页
```

目标：

```text
管理员看到报修详情后，可以一键生成 AI 分析。
```

### 页面状态

```js
data() {
  return {
    aiLoading: false,
    aiAnalysis: null,
    aiError: ''
  }
}
```

### 初始化读取最新分析

```js
async loadLatestAiAnalysis() {
  try {
    const res = await getLatestRepairAiAnalysis(this.repairId)
    this.aiAnalysis = res.data
  } catch (e) {
    this.aiAnalysis = null
  }
}
```

### 点击 AI 分析

```js
async handleAnalyzeRepair() {
  this.aiLoading = true
  this.aiError = ''

  try {
    const res = await analyzeRepairByAi(this.repairId)
    this.aiAnalysis = res.data
  } catch (error) {
    this.aiError = getAiErrorMessage(error)
  } finally {
    this.aiLoading = false
  }
}
```

### 展示字段

```text
summary：AI 摘要
category：分类
priority：优先级
urgencyLevel：紧急程度
riskLevel：风险等级
recommendedTeam：推荐班组
suggestedAction：建议处理动作
extractedLocation：提取位置
suggestedResponseMinutes：建议响应时间
safetyTips：安全提示
matchedKeywords：命中关键词
confidence：置信度
manualReviewNeeded：是否需要人工复核
provider：RULE / SPRING_AI
```

### safetyTips 解析

后端当前工单 AI 的 `safetyTips` 是 JSON 字符串，前端可以这样兼容：

```js
function parseSafetyTips(value) {
  if (!value) return []
  if (Array.isArray(value)) return value
  try {
    return JSON.parse(value)
  } catch (e) {
    return String(value).split(/[,，;；]/).filter(Boolean)
  }
}
```

### 按钮设计

```text
生成 AI 分析
重新分析
采用建议
人工修改
查看历史
```

第一版可以先做：

```text
生成 AI 分析
重新分析
查看历史
```

`采用建议` 后端还没有专用接口，后续再补。

## 六、页面三：后台客服 RAG 测试页

建议页面路径：

```text
/admin/ai/customer-service
```

目标：

```text
管理员测试知识库问答效果。
```

### 页面状态

```js
data() {
  return {
    form: {
      communityId: 1,
      question: '',
      topK: 3
    },
    loading: false,
    answer: null,
    errorMessage: ''
  }
}
```

### 提问方法

```js
async ask() {
  if (!this.form.question) {
    uni.showToast({ title: '请输入问题', icon: 'none' })
    return
  }

  this.loading = true
  this.errorMessage = ''

  try {
    this.answer = await askCustomerService(this.form)
  } catch (error) {
    this.errorMessage = getAiErrorMessage(error)
  } finally {
    this.loading = false
  }
}
```

### 展示规则

```text
cannotAnswer=false：
展示 answer、followUpActions、sources。

cannotAnswer=true：
展示 answer。
突出“转人工”按钮。
不展示“AI 已确认”之类文案。

sources 不为空：
展示“参考资料”折叠区。
显示 sourceType、title、excerpt、score。
```

## 七、页面四：知识库管理页

建议页面路径：

```text
/admin/ai/knowledge
```

第一版功能：

```text
分页查询知识
新增知识
编辑知识
禁用知识
同步公告
测试提问
```

### 表格字段

```text
id
sourceType
sourceId
communityId
title
status
visibility
effectiveTime
expireTime
updateTime
```

### 表单字段

```text
sourceType：FAQ / PROPERTY_POLICY / REPAIR_PROCESS / PROPERTY_PROCESS
sourceId：可选，不填后端可生成
communityId：可选
title：必填
content：必填
keywords：数组或逗号分隔
status：ENABLED / DISABLED
visibility：RESIDENT / STAFF / ALL
effectiveTime：可选
expireTime：可选
```

### 页面建议

```text
左侧：知识列表
右侧：新增/编辑表单
底部：测试提问区域
```

管理员新增 FAQ 后，可以直接在同页面调用 RAG 问答接口验证命中效果。

## 八、菜单和权限建议

后台菜单建议新增：

```text
AI 助手
  - 运营周报
  - 客服问答
  - 知识库管理
```

报修详情页不需要单独菜单，只是在原页面里增加 AI 卡片。

权限建议：

```text
超级管理员：可以访问全部 AI 页面。
社区管理员：只能查看本社区运营周报和知识。
普通业主：后续只开放业主端 AI 客服。
```

第一版如果权限系统还没细化，可以先只在后台菜单显示给管理员。

## 九、交互状态规范

### loading

AI 接口必须有明显 loading。

建议文案：

```text
AI 正在分析，请稍候...
大模型生成可能需要 10-30 秒
```

### timeout

如果前端超时：

```text
AI 生成耗时较长，请稍后重试。
```

不要直接显示：

```text
request:fail timeout
```

### Provider 网络波动

后端日志出现：

```text
Connection reset
```

前端可以统一显示：

```text
大模型服务网络波动，请稍后重试。
```

### 低置信度

```text
confidence < 65：仅供参考
65 <= confidence < 85：建议人工确认
confidence >= 85：可信度较高
```

### 需要人工复核

```text
manualReviewNeeded = true
```

前端必须醒目展示：

```text
需要人工复核
```

## 十、联调顺序

建议按这个顺序联调：

```text
1. 启动 AiServiceApplication，端口 8090。
2. 启动 GatewayApplication，端口 80。
3. 前端 baseURL 改成 gateway。
4. request timeout 改成 AI 接口 60000。
5. 先调运营周报 from-db 接口。
6. 再调客服 RAG ask 接口。
7. 最后调工单 AI analyze/latest 接口。
```

验证 URL：

```text
http://localhost/api/ai/operations/weekly-report/from-db?communityId=1&startDate=2026-06-01&endDate=2026-06-07
```

如果不走 gateway：

```text
http://localhost:8090/api/ai/operations/weekly-report/from-db?communityId=1&startDate=2026-06-01&endDate=2026-06-07
```

## 十一、验收标准

### 运营周报页

```text
可以选择日期范围。
可以生成周报。
sourceData 有指标卡片展示。
report 有摘要、风险、建议动作展示。
高风险周 manualReviewNeeded=true。
前端不会 10 秒就 timeout。
```

### 报修详情页 AI 卡片

```text
可以读取 latest。
可以点击 analyze。
可以展示推荐班组、紧急程度、安全提示。
可以展示 provider 和 confidence。
```

### 客服 RAG 页

```text
可以输入问题。
可以展示 answer。
可以展示 sources。
cannotAnswer=true 时显示转人工。
```

### 知识库页

```text
可以新增 FAQ。
可以查询知识列表。
新增后可以立刻通过客服 RAG 命中。
```

## 十二、推荐开发顺序

最稳的顺序：

```text
第 1 天：改 request timeout 和 baseURL，做运营周报页。
第 2 天：做报修详情页 AI 卡片。
第 3 天：做客服 RAG 测试页。
第 4 天：做知识库管理页。
第 5 天：补 UI 细节、异常提示、截图和演示数据。
```

作品展示时优先演示：

```text
1. 高风险运营周报：体现 AI 风险总结。
2. 报修漏水分析：体现工单自动分派。
3. 电动车充电问答：体现 RAG 和引用来源。
```

## 十三、后续增强方向

后续可以继续做：

```text
AI 周报保存和导出
AI 周报异步生成和轮询
工单 AI 建议采纳记录
客服问答记录和满意度评价
无法回答问题一键转 FAQ
RAG 向量数据库检索
AI Provider 管理后台
```

第一版不要一次做太满，先把“能看见、能点击、能解释链路”的页面做出来。
