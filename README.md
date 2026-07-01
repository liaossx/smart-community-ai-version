# 智慧社区微服务系统 (Smart Community)

## 项目简介

面向社区物业管理场景的微服务项目，涵盖物业缴费、活动报名、停车管理、工单处理等传统业务模块，并在此基础上独立设计实现了一个 **AI 智能服务模块**，将大模型能力引入社区管理场景。

## 技术栈

| 层级 | 技术 |
|---|---|
| 框架 | Spring Boot 2.7.18 / 3.5.14（AI 模块） + Spring Cloud Alibaba 2021.0.5.0 |
| 注册 & 配置中心 | Nacos |
| 网关 | Spring Cloud Gateway + Sentinel 限流 |
| 数据库 | MySQL 8.0 |
| 缓存 & 锁 | Redis 6.2 |
| 消息队列 | RabbitMQ 3.9 |
| AI 模型 | DeepSeek V4（通过 Spring AI 调用） |
| 容器化 | Docker + Docker Compose |

## 项目结构

```
smart-community-ai-version/
├── gateway-service/       # API 网关（路由转发、Sentinel 限流）
├── common-module/          # 公共模块（JWT、Redis 锁工具、MQ 常量）
├── user-service/           # 用户服务
├── house-service/          # 房屋服务
├── parking-service/        # 停车服务（车位管理、道闸出入、计费）
├── property-service/       # 物业服务（缴费、投诉、公告、访客）
├── workorder-service/      # 工单服务
├── community-service/      # 社区互动（活动报名）
├── system-service/         # 系统管理（日志、统计、配置）
├── ai-service/             # AI 智能服务（Spring Boot 3.5 + Java 17）
│   ├── customer/           #   RAG 智能客服
│   ├── workorder/          #   工单智能分析
│   ├── operations/         #   运营洞察 & 周报
│   ├── knowledge/          #   知识库管理 & Embedding
│   └── observability/      #   AI 调用可观测性
├── nacos_config/           # Nacos 配置文件
├── sql/                    # 数据库初始化 & AI 模块建表
├── nginx/                  # Nginx 前端配置
├── docs/                   # 项目文档 & 面试准备笔记
└── docker-compose.yml      # 一键启动所有基础设施
```

## 快速启动

### 1. 基础设施（Docker Compose）

```bash
docker-compose up -d
```

启动后自动创建 MySQL（端口 3306）、Redis（6379）、Nacos（8848）、RabbitMQ（5672）、Nginx（80）。

MySQL 首次启动自动执行 `sql/init_all.sql` 建表。

### 2. AI 模块（需额外建表 + 环境变量）

AI 服务依赖独立的表结构，需手动执行：

```bash
mysql -u root -p1234 smart_community < sql/ai_knowledge_schema_v1.sql
mysql -u root -p1234 smart_community < sql/ai_knowledge_embedding_schema_v1.sql
mysql -u root -p1234 smart_community < sql/ai_call_log_schema_v1.sql
```

AI 服务需要的环境变量：

```bash
AI_OPENAI_API_KEY=你的DeepSeek_API_Key
AI_OPENAI_BASE_URL=https://api.deepseek.com
AI_OPENAI_CHAT_MODEL=deepseek-v4-flash
AI_EMBEDDING_PROVIDER=hash          # 开发阶段用 hash，上线切 openai
```

### 3. Nacos 配置导入

Nacos 启动后（http://localhost:8848/nacos，账号 nacos/nacos）：

1. 在 DEFAULT_GROUP 下创建 DataId: `core-service.yaml`，内容复制 `nacos_config/core-service.yaml`
2. Sentinel 限流规则通过 Sentinel 控制台（8858）或直接在 Nacos 创建 `sentinel-gw-flow-rules`

### 4. 服务启动顺序

```
Gateway → System → User → House → Property / Workorder / Parking / Community → AI
```

AI 服务最后启动——它不依赖其他业务服务，只依赖 DeepSeek API 和 MySQL。

## AI 模块能力一览

| 模块 | 端点 | 功能 |
|---|---|---|
| RAG 智能客服 | `/api/ai/community/customer-service/ask` | 混合检索（关键字 + 向量）→ DeepSeek 回答 → Normalizer 校验 |
| 工单智能分析 | `/api/ai/workorder/analyze` | 结构化分类 + 优先级校准 + 高危词二次判定 |
| 运营洞察 | `/api/ai/operations/insights` | 运营指标 → AI 风险洞察 + 规则兜底 |
| 运营周报 | `/api/ai/operations/weekly-report` | 指标聚合 → AI 周报 + 模板兜底 |
| 知识库管理 | `/api/ai/knowledge/documents` | 知识文档 CRUD、Embedding 重建 |

## 关键技术点

- **RAG 混合检索**：关键字匹配 + 向量余弦相似度，加权合并 `max(关键字, 向量×0.35)`
- **Lua 脚本原子操作**：报名接口用 Lua 将查重、初始化库存、扣库存、标记用户合并为一次 Redis EVAL
- **Normalizer 输出校验**：模型回答后白名单过滤 citations、限幅 confidence、补全空字段
- **TransactionTemplate 先锁后事务**：出闸接口改编程式事务，消除分布式锁与 @Transactional 的间隙
- **Fallback 降级策略**：AI 不可用时自动切换规则引擎保底输出
- **定时缓存预热**：车位余量每 5 分钟主动刷入 Redis，首查 3ms
- **AI 可观测性**：每次大模型调用记录 bizType、耗时、token 消耗、成功/降级/失败状态

## 端口说明

| 服务 | 端口 |
|---|---|
| Gateway | 80 |
| AI Service | 8090 |
| Nacos | 8848 |
| Sentinel | 8858 |
| RabbitMQ | 5672（管理面板 15672） |
| MySQL | 3306 |
| Redis | 6379 |

## 面试准备

项目 `docs/` 目录下整理了面试笔记：

| 文件 | 内容 |
|---|---|
| `internship-interview-questions.md` | 44 道小厂高频八股题库 |
| `internship-interview-answers.md` | 对应标准答案 |
| `interview-prep.md` | 12 个话题模拟问答 |
| `jvm-notes.md` | JVM 知识笔记 |
| `threadpool-jvm-notes.md` | 线程池 + JVM 知识笔记 |
| `sentinel-notes.md` | Sentinel 笔记 |
| `rocketmq-rabbitmq-notes.md` | RocketMQ vs RabbitMQ |
| `mysql-redis-interview.md` | MySQL + Redis 面试题 |
| `ai-interview-prep.md` | AI 模块面试准备 |

## 常见问题

- **包名错误**：原 `core-service.yaml` 中包含 `com.lsx.core` 包名，与实际业务服务 `com.lsx.{service}` 不符，已在 `nacos_config/core-service.yaml` 中修正
- **RabbitMQ 消息**：消费端做了幂等处理（唯一索引），重复投递不会重复创建记录
- **支付回调被拒绝**：检查 Nacos 中的 `payment.secret` 与回调端签名是否一致
- **AI 服务调用超时**：DeepSeek thinking mode 一次调用 10-30 秒，前端 timeout 需适配
