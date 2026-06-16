# 线程池 与 JVM 概念笔记

---

## 一、线程池

### 是什么

一个装着一堆线程的池子。需要线程干活的时候从池子里拿，用完还回去，不是每次 new 一个新的。

### 为什么要有它

- 线程的创建和销毁是重的——反复 new 线程浪费资源
- 池子里的线程复用，省了反复创建的开销
- 池子大小有限，天然限流，不会无限开线程把系统拖垮

### 五个核心参数

```
        提交任务
           │
           ▼
    有空闲核心线程？──是──▶ 执行
           │ 否
           ▼
    队列满了？
      │ 没满 ──▶ 进队列等着
      │ 满了
      ▼
    有空闲最大线程？──是──▶ 新建线程执行
      │ 否
      ▼
    走拒绝策略
```

| 参数 | 含义 | 你项目里 |
|---|---|---|
| corePoolSize | 常驻线程数，不忙也活着 | 看任务类型定 |
| maximumPoolSize | 最多开多少线程 | 比 corePoolSize 大 |
| keepAliveTime | 超过 corePoolSize 的空闲线程多久回收 | 一般设 60 秒 |
| 工作队列 | 核心线程忙时任务排队的地方 | 一定有界，防 OOM |
| 拒绝策略 | 队列满了线程也满了怎么办 | CallerRunsPolicy |

### 怎么定 corePoolSize

| 任务类型 | 特征 | 配置 |
|---|---|---|
| CPU 密集 | 纯计算，不等 I/O | CPU 核数 + 1 |
| IO 密集 | 大部分时间在等 DB / 网络 / 文件 | CPU 核数 × 2 到 4 |

### 你项目里的线程池

| 在哪 | 底层 | 谁管的 |
|---|---|---|
| Tomcat 处理 HTTP | 内置线程池，默认 200 线程 | Spring Boot 自动配置 |
| RabbitMQ 消费端 | SimpleMessageListenerContainer | Spring 管理 |
| @Scheduled 定时任务 | ThreadPoolTaskScheduler，默认单线程 | Spring 管理 |

### 线程池的坑：无界队列导致 OOM

```
newFixedThreadPool(10) → 底层队列 LinkedBlockingQueue（无界）
  → 塞了 10 万个任务排队 → 每个任务对象都在堆里 → 堆满了 → OOM

安全写法：用有界队列 new LinkedBlockingQueue<>(100)
```

---

## 二、JVM

### 是什么

Java 虚拟机。你写的 .java 代码编译成 .class 字节码，JVM 读字节码帮你跑。不管你在 Windows 还是 Linux 上写 Java，只要装了 JVM，同一份 .class 都能跑。

### 堆内存

JVM 内部最大的一块内存区域。你写代码时所有 `new` 出来的对象都放在堆里。

```java
new ParkingOrder()   → 堆里多了一个 ParkingOrder 对象
new String("粤B")    → 堆里多了一个 String 对象
new ArrayList<>()    → 堆里多了一个 ArrayList 对象

// 数据库连接池、Redis 连接池、线程池 — 全是 new 出来的 → 全在堆里
```

### 堆的两半

```
        堆
    ┌─────────┐
    │ 年轻代   │ ← 短命对象：一次请求的 DTO、字符串拼接、临时变量
    ├─────────┤
    │ 老年代   │ ← 长命对象：Spring Bean、连接池、Mapper
    └─────────┘
```

- 对象先在年轻代。每次 GC 活下来的年龄 +1。到 15 岁搬进老年代
- 年轻代的 GC 叫 Minor GC：频繁（几秒一次）但快（十几毫秒）
- 老年代的 GC 叫 Full GC：偶尔（正常情况几小时一次）但慢（几百毫秒，程序会停）

### GC 做了什么

找出堆里没用的对象，清掉，腾空间。

| GC | 什么时候用 | 开启参数 |
|---|---|---|
| G1 | 堆 1-64G，默认 GC | -XX:+UseG1GC |
| Serial | 堆 < 100M，单线程反而快 | -XX:+UseSerialGC |

### OOM = 堆满了

OutOfMemoryError。堆里对象太多了，GC 清了半天还是满的，崩了。

**排查三步**：

```
1. 启动参数加：-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump.hprof
2. 崩了拿到 .hprof 文件（dump = 堆的快照）
3. MAT 打开 → Dominator Tree → 谁占最多追谁
```

**你项目里 OOM 的可能原因**：

- 线程池用了无界队列，高峰流量任务对象排了几万个
- ThreadLocal 用完没 remove，线程池复用线程，旧数据一直堆着

### 堆大小参数

```
-Xms256m    堆初始大小。JVM 启动就占这么多
-Xmx256m    堆最大大小

生产环境设一样大，避免扩容抖动
```

---

## 三、线程池 和 JVM 的那条连线

```
线程池的队列（BlockingQueue）
  → 是 new 出来的 Java 对象
  → 在堆里

队列里的任务对象
  → 也是 Java 对象
  → 也在堆里

无界队列 + 堆 256MB → 任务排了几万个 → 堆满了 → OOM
有界队列 + 堆 256MB → 最多排 100 个 → 堆安全
```

**线程池的参数决定了在极端流量下堆里要堆多少对象。这就是它们之间的那条连线。**
