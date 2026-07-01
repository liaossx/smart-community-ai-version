# AI 客服打字机效果 - 前端修改指引

## 原始文件

`owner/pages/customerService/ai-chat.vue`

---

## 改什么

在后端不改的情况下，前端拿到完整回答后逐字渲染，模拟流式输出效果。

共改 3 处：

1. `data()` 里新增 5 个字段
2. `askQuestion` 方法替换为新逻辑
3. `finally` 块加判断（打字机在跑时不重置 asking）

---

## 改动 1：data()

在 `quickQuestions: [...]` 后面加逗号，插入：

```javascript
// ===== 打字机效果 =====
typingMessage: null,    // 当前正在打字的消息对象
typingTimer: null,      // setInterval 引用
typingSpeed: 35,        // 每字间隔(毫秒)，数值越小越快
typingFullText: '',     // 完整回答文本
typingIndex: 0          // 当前打到第几个字
```

---

## 改动 2：askQuestion 方法（完整替换）

```javascript
async askQuestion() {
  const text = this.question.trim()
  if (!text || this.asking) return

  this.messages.push({ role: 'user', content: text })
  this.saveConversation()
  this.question = ''
  this.asking = true
  this.bumpScroll()

  try {
    const data = await askCustomerService({
      communityId: this.communityId ? Number(this.communityId) : undefined,
      question: text,
      topK: 3
    })

    // ===== 打字机效果：先创建空消息，然后逐字显示 =====
    const fullText = data.answer || '暂时没有找到明确答案，请联系物业客服确认。'
    const typingMsg = {
      role: 'assistant',
      content: '',
      cannotAnswer: Boolean(data.cannotAnswer),
      sources: Array.isArray(data.sources) ? data.sources.slice(0, 3) : [],
      actions: Array.isArray(data.followUpActions) ? data.followUpActions : []
    }
    this.messages.push(typingMsg)
    this.bumpScroll()

    this.typingMessage = typingMsg
    this.typingFullText = fullText
    this.typingIndex = 0

    this.typingTimer = setInterval(() => {
      if (this.typingIndex < this.typingFullText.length) {
        this.typingMessage.content += this.typingFullText[this.typingIndex]
        this.typingIndex++
        if (this.typingIndex % 3 === 0) {
          this.bumpScroll()
        }
      } else {
        clearInterval(this.typingTimer)
        this.typingTimer = null
        this.typingMessage = null
        this.typingFullText = ''
        this.typingIndex = 0
        this.asking = false
        this.bumpScroll()
        this.saveConversation()
      }
    }, this.typingSpeed)

  } catch (error) {
    console.error('业主端 AI 客服问答失败:', error)
    this.messages.push({
      role: 'assistant',
      content: getAiErrorMessage(error),
      cannotAnswer: true
    })
  } finally {
    if (!this.typingTimer) {
      this.asking = false
      this.saveConversation()
      this.bumpScroll()
    }
  }
},
```

---

## 原理说明

| 步骤 | 代码 | 说明 |
|-----|------|------|
| 创建空消息 | `this.messages.push({ role: 'assistant', content: '' })` | 先把壳子推入列表，内容为空 |
| 记录上下文 | `this.typingMessage = typingMsg` | 记录消息引用，后续逐字追加 |
| 启动定时器 | `setInterval(() => { ... }, 35)` | 每 35ms 执行一次 |
| 逐字追加 | `this.typingMessage.content += fullText[index]` | 每 tick 追加一个字 |
| 完成清理 | `clearInterval` + `asking = false` | 全打完后收尾 |

typingSpeed 可调：`20` 更快、`50` 适中、`100` 更慢。

---

## 不改什么

- template 模板不动（消息列表天然支持 content 动态变化）
- style 样式不动
- 后端不动（仍然一次性返回完整 JSON）

---

## 与真正 SSE 流式输出的区别

| | 打字机（本次改动） | SSE 流式输出 |
|---|---|---|
| 后端 | 不改，等完全算完再返回 | 改，stream=true |
| 首字延迟 | 等整段算完才开始打字 | 第一个 token 出来就显示 |
| 用户体验 | 看起来像打字（实际被等） | 真正边算边出 |
| 实现成本 | 5 分钟前端改 | 需要后端配合改 Streaming |

如果以后要做 SSE，把 `askCustomerService` 的 HTTP 请求改成 EventSource 连接即可。