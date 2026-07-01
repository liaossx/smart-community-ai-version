# Claude Code Skills 使用教程

## 一、什么是 Skills？

Skills 是 Claude Code 中的 **可复用 AI 指令**，本质上是一个 `SKILL.md` 文件。你可以把重复的工作流程、项目规范、领域知识编码进 Skill 中，让 Claude Code 自动调用或通过 `/command` 唤起。

**核心特点：**
- **渐进披露**：Claude 只加载名称和描述（约 30-50 tokens），触发时才加载完整指令
- **可版本控制**：项目级 Skill 可以提交到 Git，团队共享
- **自动/手动触发**：既可以斜杠命令调用，也可以让 Claude 根据上下文自动匹配

---

## 二、Skill 的存放位置

| 位置 | 作用域 | 说明 |
|------|--------|------|
| `.claude/skills/<name>/SKILL.md` | 单项目 | 随 Git 提交，团队共享 |
| `~/.claude/skills/<name>/SKILL.md` | 全局（用户级） | 所有项目可用，个人专属 |
| 插件内部 | 全局 | 通过插件市场分发 |

> **推荐**：项目特有的 Skill 放在 `.claude/skills/` 下提交到仓库；个人工作流放在 `~/.claude/skills/`。

---

## 三、快速创建第一个 Skill

### 步骤 1：创建目录

```bash
# 项目级 skill
mkdir -p .claude/skills/explain-code

# 用户级 skill
mkdir -p ~/.claude/skills/explain-code
```

### 步骤 2：编写 SKILL.md

```markdown
---
name: explain-code
description: 用比喻和 ASCII 图示解释代码逻辑，当用户问"这段代码怎么工作的"时触发
argument-hint: "[file-path]"
---

# 代码解释规则

1. 先用现实生活中的类比解释核心功能（不要用术语）
2. 画出 ASCII 执行流程图
3. 逐行讲解关键代码
4. 指出常见错误和边界情况
5. 最后给出简洁总结
```

### 步骤 3：使用

```
/explain-code src/main/java/com/example/Service.java
```

也可以在对话中直接说"帮我解释这段代码"，Claude 会自动匹配 skill。

---

## 四、YAML 前置元数据详解

SKILL.md 头部必须包含 YAML 前置元数据：

```yaml
---
name: my-skill              # (必填) 斜杠命令名称，小写+连字符
description: 描述文字        # (必填) 用于匹配触发场景，写清楚何时触发
argument-hint: "[file]"     # (可选) 斜杠命令的参数提示文本
disable-model-invocation: true   # (可选) true=仅手动调用，适合危险操作
user-invocable: false            # (可选) false=仅自动触发，适合后台知识
allowed-tools: Read,Grep,Bash   # (可选) 权限最小化，限制可用的工具
context: fork                    # (可选) 在隔离子代理中运行，不污染主会话
agent: Explore                   # (可选) 指定子代理类型（Explore/Debug 等）
model: sonnet-4.6               # (可选) 覆盖此 skill 使用的模型
---
```

### 常见配置模式

| 模式 | 手动调用 | 自动触发 | 适用场景 |
|------|----------|----------|----------|
| 默认（不设标志） | ✅ | ✅ | 一般工具 |
| `disable-model-invocation: true` | ✅ | ❌ | 部署、删除、危险操作 |
| `user-invocable: false` | ❌ | ✅ | 后台知识/编码规范 |

---

## 五、Skill 目录结构

Skill 不只是单个文件，可以是一个目录：

```
my-skill/
├── SKILL.md            # 必填：核心指令 + YAML 头部
├── reference.md        # 可选：详细参考文档（按需加载）
├── examples.md         # 可选：使用示例
└── scripts/            # 可选：可执行脚本
    ├── helper.sh
    └── analyze.py
```

> **原则**：SKILL.md 只写核心指令，详细内容放到 reference.md 等文件中按需加载，保持上下文精简。

---

## 六、进阶用法

### 1. 参数传递 `$ARGUMENTS`

```yaml
---
name: fix-issue
description: 修复指定的 GitHub Issue
disable-model-invocation: true
allowed-tools: Bash,gh
argument-hint: "[issue-number]"
---

# 修复 GitHub Issue #$ARGUMENTS

1. 查看 Issue 详情：`gh issue view $ARGUMENTS`
2. 定位相关代码
3. 实现修复方案
4. 编写测试
5. 创建 PR 引用 #$ARGUMENTS
```

**调用**：`/fix-issue 42`

### 2. 动态上下文注入 `!command`

在 Skill 中嵌入实时命令输出：

```yaml
---
name: pr-summary
description: 总结当前 PR 的变更内容
context: fork
allowed-tools: Bash(gh:*)
---

## PR 摘要

- 变更内容：!`gh pr diff`
- 评论：!`gh pr view --comments`
- 变更文件：!`gh pr diff --name-only`

请总结关键变更、风险和测试状态。
```

### 3. 隔离子代理 `context: fork`

在独立子代理中运行，避免主会话上下文被污染：

```yaml
---
name: deep-research
description: 深度调研某个代码模块
context: fork
agent: Explore
allowed-tools: Read,Grep,Glob
argument-hint: "[module-path]"
---

深入调研 $ARGUMENTS 模块...

1. 查找所有相关文件
2. 分析核心逻辑和数据流
3. 绘制依赖关系
4. 输出结果（包含文件路径和行号）
```

### 4. 后台知识 Skill（无斜杠命令）

这种 Skill 不会出现在 `/skills` 命令列表中，但 Claude 会自动根据描述匹配触发：

```yaml
---
name: api-conventions
description: 项目的 REST API 设计规范，当讨论 API 设计时触发
disable-model-invocation: true
user-invocable: false
---

# API 设计规范

- 路径：kebab-case（`/api/user-info`）
- 参数：camelCase（`userId`）
- 分页：`page=1, size=10`
- 响应格式：`{ code, message, data }`
- 错误码：4xx 客户端错误，5xx 服务端错误
```

---

## 七、在 Claude Code 中验证和管理

| 命令 | 作用 |
|------|------|
| `/skills` | 列出所有已加载的 Skill |
| `/my-skill 参数` | 手动调用 Skill |
| `/permissions list Skill` | 查看 Skill 的权限设置 |
| 重启会话 | 重新加载 Skill 文件 |

---

## 八、最佳实践

### 1. 不要写 Claude 已经知道的
Skill 的价值在于**项目特有、非显而易见**的知识。Claude 已经懂编程，不需要教它写代码。

### 2. 记录 Gotchas（易错点）
把团队踩过的坑记在 Skill 里，这是最有价值的内容。比如："这个 API 的排序参数是 `sort_order` 不是 `order`"。

### 3. 渐进披露
SKILL.md 保持精简，细节放在 `reference.md` 中：

```
my-skill/
├── SKILL.md         ← 核心指令 + 索引
└── reference.md     ← 详细参考，Claude 按需加载
```

### 4. 描述写给 AI 看，不是写给人看
description 字段是 Claude 判断是否触发 Skill 的依据。要具体、带有触发条件：

```yaml
# ❌ 不明确
description: 代码审查

# ✅ 明确触发条件
description: 审查 Go 代码变更，检查错误处理和并发安全性，当讨论 pull request 时触发
```

### 5. 权限最小化
始终限制 `allowed-tools`，特别是危险操作的 Skill：

```yaml
# ❌ 权限过大
allowed-tools: *

# ✅ 最小权限
allowed-tools: Bash(gh:*),Read
```

### 6. 从简单开始
先用一个 Skill 解决你每天重复的任务（如每日站会、代码审查、PR 总结），然后逐步迭代完善。

---

## 九、Skills vs 其他扩展机制

| 特性 | 适用场景 | Token 开销 |
|------|----------|------------|
| **Skills** | 重复流程、领域知识、项目规范 | ~30-50 tokens/个（渐进加载） |
| **MCP** | 外部工具连接（数据库、浏览器等） | 可高达 50k+ tokens |
| **Hooks** | 确定性自动化操作（自动 lint、发通知） | 极小 |
| **Plugins** | Skills + MCP + Hooks 打包分发 | 总和 |

---

## 十、常见问题排查

| 问题 | 解决方法 |
|------|----------|
| Skill 不触发 | 检查目录结构是否正确，会话中运行 `/skills` 确认已加载 |
| 触发太频繁 | 让 `description` 更具体，或设 `disable-model-invocation: true` |
| `$ARGUMENTS` 没有替换 | 检查占位符拼写是否正确，确认调用时传了参数 |
| Skill 太大无法加载 | 将详细内容移到 `reference.md`，可设环境变量 `SLASH_COMMAND_TOOL_CHAR_BUDGET=30000` |
| Skill 不在 `/skills` 中 | 重启 Claude Code 会话，检查文件路径和权限 |
| Skill 权限不足 | 运行 `/permissions list Skill` 查看并调整 |

---

## 十一、实战案例：为这个项目创建 Skill

以下是一些适合本项目（smart-community-ai-version）的 Skill 示例：

### 示例 1：模块间调用规范

```yaml
---
name: module-conventions
description: 本项目微服务模块间的调用规范和常见注意事项
disable-model-invocation: true
user-invocable: false
---

# 模块调用规范

- 模块间通过 gateway-service 路由，禁止直接调用其他模块的 API
- Feign 接口定义在 common-module 中
- 服务发现使用 Nacos
- 所有 API 响应格式统一为 R 对象
```

### 示例 2：启动项目

```yaml
---
name: start-project
description: 启动整个微服务项目（Docker + 各模块）
argument-hint: "[module-name:可选，只启动特定模块]"
---

# 启动项目

## 全量启动
```bash
docker-compose -f docker-compose.infra.yml up -d
# 等待基础设施就绪后
mvn clean install -DskipTests
```

## 单独启动某个模块
参考 README.md 中的启动说明。
```

### 示例 3：代码审查规范

```yaml
---
name: review-guide
description: 本项目代码审查的 checklists 和规范，审查代码时自动触发
disable-model-invocation: true
user-invocable: false
---

# Code Review Checklist

## 通用
- 所有 API 返回 R 统一响应
- 事务注解 @Transactional 使用正确
- 日志使用 SLF4J，不直接使用 System.out

## 安全
- SQL 使用 MyBatis Plus 参数占位符，禁止拼接字符串
- 用户输入必须校验 (@Valid)
- 敏感数据脱敏处理

## 数据库
- 表名使用小写+下划线
- 索引命名规范：idx_表名_字段名
```
