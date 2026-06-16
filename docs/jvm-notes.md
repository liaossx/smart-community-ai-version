# JVM 知识笔记

---

## 一、堆内存

**是什么**：Java 里所有 new 出来的对象都放在堆里。是你程序运行时占内存最大的那块区域。

```
你写的代码                      堆
────────────────────────────────────────
new ParkingOrder()    →  一个 ParkingOrder 对象
new String("粤B")     →  一个字符串对象
new ArrayList<>()     →  一个 ArrayList 对象
```

**不是堆里的**：方法里的基本类型变量（int、long、boolean）在栈上，不在堆里。对象的引用变量也在栈上，但对象本身在堆里。

---

## 二、堆的大小参数

```
-Xms256m    堆的初始大小。JVM 启动时就占这么多。
-Xmx256m    堆的最大大小。堆最多涨到这么大。

生产环境建议 Xms = Xmx，避免堆扩容缩容时的停顿。
为什么 256MB？Spring Boot 微服务起步够用。
```

---

## 三、堆的内部分区

```
                堆（仓库）
    ┌──────────────────────────┐
    │  年轻代（占 1/3）         │ ← 放短命对象
    │  ├─ Eden（伊甸园）        │   新对象先来这
    │  ├─ Survivor 0           │   熬过一次 GC 搬到这
    │  └─ Survivor 1           │   两区来回倒
    ├──────────────────────────┤
    │  老年代（占 2/3）         │ ← 放长命对象
    └──────────────────────────┘
```

**年轻代放什么**：一次请求的临时对象。方法里 new 的 DTO、字符串拼接、返回值。
**老年代放什么**：活很久的对象。Spring Bean、数据库连接池、Redis 连接池、Mapper。

**怎么从年轻代到老年代**：每次 Minor GC 活下来的对象，年龄 +1。默认到 15 岁搬进老年代。

---

## 四、GC = 垃圾回收 = 自动清理工

```
Minor GC（清理年轻代）
  → 频繁（几秒一次）
  → 快（十几毫秒）
  → 你感知不到

Full GC（清理老年代）
  → 很少（几小时一次，正常情况）
  → 慢（几百毫秒到几秒）
  → 程序暂停
```

GC 触发时，所有工作线程暂停——Minor GC 暂停几毫秒感知不到，Full GC 暂停几百毫秒用户点按钮就卡顿了。

---

## 五、GC 选择

| GC | 什么时候用 | 开启参数 |
|---|---|---|
| G1 | 堆 1-64G，默认 GC，停顿可控 | -XX:+UseG1GC |
| Serial | 堆 < 100M 的小服务，单线程反而快 | -XX:+UseSerialGC |

Java 17 默认就是 G1，不写 `-XX:+UseG1GC` 也生效。写上更清晰。

---

## 六、OOM = 堆满了崩了

**原因 1**：线程池用无界队列，高峰流量下任务对象排了几万个，堆被打满。

**原因 2**：对象放进集合忘了删，List 越涨越大。

**原因 3**：ThreadLocal 用完没 remove。线程池复用线程，旧数据堆积。

**排查三步**：
```
1. 启动参数加上：-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump.hprof
2. OOM 发生后，把 .hprof 文件拿下来
3. 用 MAT 打开 → Dominator Tree → 谁占最多追谁
```

**dump 文件**：OOM 那一刻堆的快照，记录所有对象和引用关系。
**MAT**：Eclipse Memory Analyzer Tool，分析 dump 文件的工具。

---

## 七、ThreadLocal 和 OOM 的关系

ThreadLocal 存的值在线程对象里。如果线程池复用线程，用完不 remove，值永远清不掉。一万个请求后堆里攒了一万个废弃数据，最终 OOM。

**解决**：finally 里调 `remove()`。

---

## 八、完整启动命令模板

```
java
  -Xms256m -Xmx256m                         ← 堆大小
  -XX:+UseG1GC                               ← GC 选 G1
  -XX:MaxGCPauseMillis=200                   ← GC 单次不超过 200ms
  -XX:+HeapDumpOnOutOfMemoryError            ← OOM 自动拍快照
  -XX:HeapDumpPath=/tmp/dump.hprof           ← 快照存哪
  -Xlog:gc*:file=/tmp/gc.log:time,uptime     ← GC 日志开起来
  -jar app.jar
```

---

## 九、线程池为什么能导致 OOM

```
线程池 = 核心线程 + 工作队列

newFixedThreadPool(10)  → 底层队列 LinkedBlockingQueue（无界）
  → 10000 个请求进来 → 10 个在跑，9990 个排队
  → 排队的 9990 个任务对象全在堆里 → 堆满了 → OOM

安全写法：
new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),     ← 有界！最多排 100 个
    new CallerRunsPolicy())             ← 满了调主线程自己跑
```

---

## 十、面试标准回答

> 上线前给稳妥初始值：256MB 堆、G1、GC 日志、OOM 自动 dump。跑几天看 GC 日志和 jstat。如果 Full GC 频繁就加大堆；Minor GC 频繁就把 G1 的老年代回收阈值调低让它早点回收。稳定了就不再动了。OOM 排查就三步：dump 文件 + MAT + Dominator Tree。
