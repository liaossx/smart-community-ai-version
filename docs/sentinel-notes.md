# Sentinel 笔记

---

## 一、是什么

一个流量控制工具。在请求进你的服务之前拦一道，流量大了就挡住，不让后端被打垮。

---

## 二、你项目里做了什么

**Gateway 层接口限流。** 没有做服务端的熔断降级。

三个地方：

### pom.xml（引入依赖）

```xml
<!-- Sentinel 核心 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
<!-- 适配 Gateway，自动注入过滤器 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
</dependency>
<!-- 规则存在 Nacos 上 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
</dependency>
```

### application.yml（配置）

```yaml
sentinel:
  filter:
    enabled: true          # 打开过滤器
  scg:
    fallback:
      response-status: 429
      response-body: ''{"code":429,"msg":"请求过于频繁，请稍后再试"}''  # 被拦时返回这个
  datasource:
    gw-flow:
      nacos:
        data-id: sentinel-gw-flow-rules   # 规则从 Nacos 动态拉，改不重启
        rule-type: gw-flow
```

### Nacos 上的规则（JSON）

```json
[
  { "resource": "ai-service",      "grade": 1, "count": 50,  "controlBehavior": 0 },
  { "resource": "parking-service", "grade": 1, "count": 200, "controlBehavior": 0 }
]
```

| 字段 | 值 | 含义 |
|---|---|---|
| resource | 路由 ID | 对哪个服务限流 |
| grade | 1 | QPS 限流 |
| count | 50 / 200 | 每秒最多几个请求 |
| controlBehavior | 0 | 超了直接拒绝 |

---

## 三、一条请求走 Sentinel 的完整流程

```
GET /api/ai/community/customer-service/ask
  │
  ▼
Gateway 匹配路由 → ai-service
  │
  ▼
Sentinel 过滤器拦截 → 查 Nacos 上的规则
  │
  ├─ 这一秒请求 < 50 → 放行，转发到 ai-service
  └─ 这一秒请求 ≥ 50 → 挡回去，返回 429 + "请求过于频繁，请稍后再试"
```

---

## 四、概念拆解

### 过滤器

请求进来之后、到业务代码之前，中间拦的一道检查。`spring-cloud-alibaba-sentinel-gateway` 依赖一引入，Spring 自动创建这个过滤器挂在 Gateway 上。不需要自己写 Java。

### QPS

Queries Per Second = 每秒请求数。`count = 50` 意思是这条路由每秒最多放行 50 个请求。

### 限流 (Rate Limiting)

流量太大，直接拒绝多余请求。防别人打你。

### 降级 (Fallback / Degradation)

被限流时不用返回报错页面，返回一个兜底结果——"请求过于频繁，请稍后再试"。用轻量响应替代真实业务处理。

### 熔断 (Circuit Breaking)

跟限流不同。限流是"流量大了拒绝"，熔断是"下游挂了就断开，等它恢复"。

```
调 DeepSeek → 连续失败 5 次 → Sentinel 熔断
  → 后面 30 秒的请求直接走降级方法，不调 DeepSeek
  → 30 秒后半开 → 放一个请求试试
    → 成功了 → 恢复
    → 还失败 → 继续熔断
```

你项目里没做熔断——目前只有网关层限流。

---

## 五、两种用法

### 用法一：网关层限流（你做的）

```
请求 → Gateway → Sentinel → 超了返回 429 → 没超转发下游
```

规则写在 Nacos 上，不用写 Java。按路由维度控整体 QPS。颗粒度粗，简单粗暴。

### 用法二：服务层限流 / 熔断（你没做，但常见）

```java
@SentinelResource(
    value = "getRemaining",
    blockHandler = "handleBlock",   // 被限流或熔断时走这
    fallback = "handleError"        // 方法自己抛异常时走这
)
public ParkingSpaceRemainVO getRemaining(Long communityId) { ... }

// blockHandler：Sentinel 主动拦的（限流 / 熔断）—— 原方法没执行
public ParkingSpaceRemainVO handleBlock(Long id, BlockException e) {
    return new ParkingSpaceRemainVO();  // 返回空对象兜底
}

// fallback：原方法执行了但中途抛异常 —— 比如 DB 挂了
public ParkingSpaceRemainVO handleError(Long id, Throwable e) {
    log.error("查询失败", e);
    return new ParkingSpaceRemainVO();  // 返回空对象兜底
}
```

| | blockHandler | fallback |
|---|---|---|
| 谁触发的 | Sentinel（限流或熔断） | 业务代码自己抛了异常 |
| 原方法执行了吗 | 没，被拦了 | 执行了，但炸了 |

### 网关 vs 服务层

| | 网关层限流 | 服务层熔断 |
|---|---|---|
| 在哪拦 | Gateway，请求还没到业务 | Service 方法，已经进了业务代码 |
| 能干嘛 | 限流 | 限流 + 熔断 + 降级 |
| 颗粒度 | 整条路由 | 单个方法 |
| 保护谁 | 保护整个后端 | 保护自己不被下游拖垮 |

---

## 六、你项目里为什么 AI 服务只设 50 QPS

DeepSeek 调用一次要十几秒。如果同时进来 200 个 AI 请求，线程池全占着等 DeepSeek 返回，后面的请求全堵死超时。50 QPS 是保护网关自身的线程资源，不是 CPU 扛不住。

---

## 七、规则改了要重启吗

不用。Gateway 通过 datasource 订阅 Nacos，Nacos 推送变更后自动生效。这就是 Sentinel 配 Nacos 的核心价值——限流规则可以实时调整。
