# RocketMQ vs RabbitMQ 笔记

---

## 一、本质区别

| | RabbitMQ | RocketMQ |
|---|---|---|
| 语言 | Erlang | Java |
| 吞吐量 | 万级/秒 | 十万级/秒 |
| 事务消息 | 不支持 | 天生支持 |
| 顺序消息 | 单队列内有序 | 全局和分区有序 |
| 延迟消息 | 插件实现 | 天生支持 18 个级别 |
| 运维 | 轻量，单机能扛 | 重，通常集群部署 |
| 社区 | 全球通用，文档极多 | 阿里出品，中文文档好 |

---

## 二、事务消息的核心区别

### 你现在的 RabbitMQ 做法

```java
// 两步独立操作，中间有缝隙
redis.execute(joinScript, ...);                    // ① Lua 扣库存
rabbitTemplate.convertAndSend(exchange, key, msg); // ② 发 MQ
// ① 和 ② 之间服务器挂了 → Redis 扣了但 MQ 没发 → 数据不一致
```

兜底方式：定时对账任务（课后补丁，不是事前预防）。

### RocketMQ 事务消息做法

```java
// 两步包在同一个回调里，用"半消息"机制消除缝隙
rocketMQ.sendMessageInTransaction(msg, new TransactionListener() {

    // 本地操作在这里执行
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        List<Object> result = redis.execute(joinScript, ...);  // Lua 扣库存
        return result.get(0) == 1 ? COMMIT_MESSAGE : ROLLBACK_MESSAGE;
    }

    // 如果 executeLocalTransaction 没执行完服务器就挂了，RocketMQ 回调这个方法确认状态
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        Boolean signedUp = redis.opsForSet().isMember("activity:users:" + id, userId);
        return signedUp ? COMMIT_MESSAGE : ROLLBACK_MESSAGE;
    }
});
```

### 原理：半消息

消息不是一步到位的，分三步：

```
① sendMessageInTransaction → 消息存为"半消息"（消费者看不见）
② 回调 executeLocalTransaction → 执行本地操作
③ 返回 COMMIT → 半消息变正式消息 → 消费者才能消费
   返回 ROLLBACK → 半消息删除 → 跟没发过一样
   服务器中途挂了 → RocketMQ 启动回查 → 调 checkLocalTransaction 确认状态
```

| | RabbitMQ | RocketMQ 事务消息 |
|---|---|---|
| 消息何时可见 | convertAndSend 之后立即可见 | 本地操作返回 COMMIT 之后才可见 |
| 两步之间 | 有缝隙 | 半消息机制消除缝隙 |
| 服务器挂了 | 消息已发，无法撤回 | 半消息在 Broker，回查确认最终状态 |
| 额外成本 | 无 | 一个回查方法 |

---

## 三、什么时候选谁

| 场景 | 选 RabbitMQ | 选 RocketMQ |
|---|---|---|
| 初创项目、中小流量 | 轻量、易部署、学习成本低 | 太重 |
| 需要事务消息 | 不支持 | 天生支持 |
| 高吞吐（每秒万级以上） | 开始吃力 | 适合 |
| 已经在用阿里云 | 可以自建 | 阿里云上有托管服务 |
| 你的智慧社区项目 | 目前够了 | 以后量大了再考虑 |

---

## 四、面试回答：为什么用 RabbitMQ 而不是 RocketMQ

**面试官问**："你项目里用了 RabbitMQ，有没有考虑过 RocketMQ？为什么选这个？"

**回答**（约 40 秒）：

> 选型上 RabbitMQ 对当前业务完全够用。社区几千户人，报名、停车、缴费，一天的消息量可能不到一万条，RabbitMQ 的万级吞吐绰绰有余。RocketMQ 吞吐更高、支持事务消息，但需要更大的部署资源——NameServer + Broker + 可能还要集群。
>
> 事务消息的缺失我用上层方案补了——Lua 脚本保证 Redis 层原子操作、MQ 消费端用唯一索引防重复消费、再加一个定时对账任务做终极校准。三层兜底下来，丢消息的概率已经极低了。选型不是选最强的，是选复杂度匹配当前风险的。

---

## 五、匿名内部类的写法

`sendMessageInTransaction` 的参数位置写了一大坨 `new TransactionListener() { ... }`，这是匿名内部类——不需要单独建 `.java` 文件，直接在调用处当场实现接口。接口只在这一个地方用一次，这样写比单独建类更省事。
