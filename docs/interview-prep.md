# 面试准备清单 — 智慧社区微服务平台

---

## 一、Redis 分布式锁

**知识点**：分布式锁的设计与实现

**技术点**：
- `SET key value NX EX 10` 原子加锁
- Lua 脚本：`GET + 比对 value + DEL` 原子释放
- 锁粒度：`lock:parking:exit:{plateNo}` 按车牌隔离
- 重试策略：5 次 × 200ms
- 锁必须放在事务外：TransactionTemplate 控制顺序

**提问**：停车场道闸重复抓拍，你怎么保证不会重复计费？

**回答**：
> 我在出闸接口上加了 Redis 分布式锁做幂等控制。锁的 key 按车牌号拼——"lock:parking:exit:" + 车牌号，粒度到单辆车，不会锁其他车的出闸。
>
> 加锁用的是 SET key value NX EX 10，拿锁失败了重试 5 次、每次等 200ms，都失败就返回"车辆正在处理中，请勿重复操作"。释放用了 Lua 脚本，先 GET 比对自己的 value，对上了才 DEL，防止锁过期后被别人删掉。
>
> 关键的设计是锁必须放在数据库事务外面。原来锁在 @Transactional 方法里，锁释放后事务还没提交，下一个线程读到旧数据就重复计费了。后面用 TransactionTemplate，流程是先拿锁、再开事务、事务提交了才释放锁，中间没有间隙。

---

**提问**：锁超时了、业务还没跑完怎么办？

**回答**：
> 当前锁的超时设了 10 秒，出闸业务一般几百毫秒就完成了，10 秒足够。如果以后出现业务耗时超过锁超时的情况，可以切 Redisson 的 Watch Dog——每 10 秒自动续期，业务跑完再停。

---

## 二、缓存穿透 / 击穿 / 雪崩

**知识点**：Redis 缓存的三大经典问题

**技术点**：
- 穿透：查不存在的数据 → 布隆过滤器 或 缓存空对象 + 短 TTL
- 击穿：热点 key 过期 + 大量并发 → 互斥锁 或 预热
- 雪崩：大量 key 同时过期 → 错开 TTL + 预热兜底
- 项目中：parkingRemain 10s / mySpaces 30s / houseInfo 5min

**提问**：你简历上写了处理过缓存穿透和击穿，具体讲讲你怎么做的？

**回答**：
> 穿透——我拿房屋详情接口举例。GET /api/house/{houseId}，houseId 是用户传的，
> 有人可以遍历不存在的 ID 来打库。当前项目里查出来 null 时不缓存，但如果攻击者
> 用大量假 ID 扫描，还是会每次都穿透到 MySQL。下一步的优化方案有两种：轻量的用
> 缓存空对象——查出来 null 也写缓存，设 30 秒过期，同一段时间内同一个假 ID 只打
> 一次 DB。如果攻击规模更大，就用布隆过滤器在缓存前面拦截——把所有合法的 houseId
> 预先写进布隆，请求先过布隆，不在集合里直接返回 404，连缓存都不用查。
>
> 击穿——热点 key 过期后大量请求同时打数据库。我用了定时预热：每 5 分钟主动把
> 全部社区的车位余量缓存塞进 Redis。用户打开页面后的第一次查询也是 3ms，不用
> 体验 292ms 的首次慢查询。
>
> 雪崩——我把不同缓存设了不同的 TTL。parkingRemain 10 秒、mySpaces 30 秒、
> houseInfo 5 分钟，错开失效时间，不会集中失效。加上预热兜底，即使有 key 碰巧
> 同时过期，也会在 5 分钟内被补回去。

---

**提问**：缓存预热具体怎么做的？

**回答**：
> 定时任务每 5 分钟跑一次。先从 MySQL 查出所有有车位的社区 ID，然后逐个调 Service 的 getRemaining 方法。这里有个巧妙的点——定时任务不直接写 Redis，而是调 Service 方法，让 @Cacheable 自己去完成"查 MySQL → 写 Redis"。逻辑不重复，缓存配置一改，预热自动跟着变。

---

## 三、缓存一致性

**知识点**：Cache-Aside 模式下缓存与数据库的一致性

**技术点**：
- 读：先查 Redis → 命中返回 / 未命中查 DB 并写回 Redis
- 写：先写 DB → 再清 Redis（不是更新）
- `@CacheEvict(allEntries = true)`：全量清除，避免漏 key
- `transactionAware()`：事务提交后才真正清缓存，缩小脏读窗口

**提问**：别人买了车位之后，缓存里的余量是旧数据怎么办？

**回答**：
> 写操作上标了 @CacheEvict(allEntries = true)，买车位后清空整个 parkingRemain 缓存。下一次查询走 DB 拿到最新数据，同时写回 Redis。
>
> 为什么是 allEntries = true？因为一个社区的车位被买，全局统计（key = 'all'）和该社区的统计（key = 1、key = 2）都变了。如果逐个 key 清除，容易漏。
>
> 极端情况下有一个极短的脏读窗口——清缓存和提交事务之间来了一个读请求。但 RedisCacheManager 设了 transactionAware()，@CacheEvict 在事务提交后才真正执行，最大程度收窄了这个窗口。对车位余量这种允许秒级延迟的场景完全够用。

---

## 四、Spring Cache + Redis 实战

**知识点**：声明式缓存框架的落地使用

**技术点**：
- `@Cacheable(cacheNames, key)`：查询方法自动缓存
- `@CacheEvict(cacheNames, allEntries)`：写入方法自动清缓存
- `@Caching(evict = {...})`：组合多个缓存操作
- `@EnableCaching` + `RedisCacheManager`：总控开关 + 按缓存名设 TTL
- `@Scheduled(cron)`：定时预热

**提问**：292ms 降到 3ms，怎么做到的？能具体讲讲缓存链路吗？

**回答**：
> 一共三层。第一层是查询缓存——在 getRemaining 方法上标 @Cacheable(cacheNames = "parkingRemain")，Spring AOP 拦到调用后先查 Redis，命中了直接返回 3ms 左右，不执行方法体里的 count(*)。
>
> 第二层是写操作清缓存——买车位的接口上标了 @CacheEvict(allEntries = true)，数据变了清空缓存，下次查询拿最新数据。
>
> 第三层是定时预热——@Scheduled 每 5 分钟调 getRemaining，提前把缓存塞满。292ms 和 3ms 是用 JMeter 同一接口同一环境测的，100 线程循环 10 次取平均值。

---

## 五、RabbitMQ 异步落库

**知识点**：消息队列在库存扣减场景中的应用

**技术点**：
- 发送端：`RabbitTemplate.convertAndSend`，publisher confirm + 消息持久化
- 消费端：`@RabbitListener` + `@Transactional`，唯一索引防重复消费
- 架构：Redis 预扣 → 快速返回 → MQ 异步写 DB
- 兜底：定时任务对账，补录差异

**提问**：活动报名场景里，消息队列具体做了什么？消息丢了怎么办？

**回答**：
> 报名流程是"Redis 预扣库存 → MQ 异步落库 → 消费端写 MySQL"。用户端只感知到 Redis 扣库存这一步，几十毫秒就返回了，后面的 DB 写入不阻塞用户。
>
> 防丢做了两层：发送端开启 publisher confirm，MQ 写不进会报错；消费端的报名表有唯一索引 uk_activity_user，消息重复投递时抛 DuplicateKeyException 直接忽略。
>
> 万一 MQ 真丢了，还有一个定时任务做对账——查数据库的报名人数和 Redis 预扣量是否一致，不一致就补录或告警。

---

## 六、Sentinel 网关限流与降级

**知识点**：微服务网关层的流量防护

**技术点**：
- Sentinel Gateway Adapter：自动注入过滤器，对每个路由做 QPS 统计
- 规则存储在 Nacos：`sentinel-gw-flow-rules`，Gateway 通过 datasource 订阅，改了不重启
- 降级响应：被限流时返回 HTTP 429 + JSON `{"code":429,"msg":"请求过于频繁"}`
- 与服务注册的关系：Gateway 从 Nacos 拿到服务列表，限流规则对每个服务独立配置

**提问**：你简历上写了 Sentinel 限流，具体怎么配的？

**回答**：
> 我在 Gateway 层做了接口限流。Gateway 引入 Sentinel Gateway Adapter 后，过滤器自动注入，每个请求进来先过 Sentinel 检查当前路由的 QPS。
>
> 规则存在 Nacos 上，JSON 格式，data-id 叫 sentinel-gw-flow-rules。Gateway 通过 datasource 订阅 Nacos，改了规则自动生效不用重启。被限流的请求不转发到下游服务，直接在网关层返回 429 和一段 JSON 提示。
>
> 限流粒度按路由 ID 配——parking-service 设 200 QPS，ai-service 只设 50 QPS。为什么 ai-service 更低？因为 AI 调用一次要十几秒走 DeepSeek，50 QPS 就能把线程池打满。

---

## 七、@Transactional 失效场景与事务-锁顺序

**知识点**：Spring 事务代理的实际踩坑经验

**技术点**：
- 失效场景：this 调用不经过代理、非 public 方法、异常被 catch 不回滚
- AOP 代理原理：`@Transactional` 跑在代理对象上，原始对象的方法调用不走事务
- 锁在事务外：TransactionTemplate 手动控制事务边界
- 顺序：拿锁 → 开事务 → 业务 → 提交事务 → 释放锁

**提问**：你说调整了事务和锁的顺序，原来有什么问题？后来怎么解决的？

**回答**：
> 原来我把分布式锁放在 @Transactional 方法里面。锁在方法体里释放了，但 @Transactional 是在方法返回后才提交事务。中间有个窗口——锁已经被释放，其他线程拿到了新锁，但前一个线程的事务还没提交，读到的是旧数据。出闸场景下就表现为重复计费。
>
> 解决方式是把 @Transactional 改成 TransactionTemplate。exitGate 方法本身不加事务，先拿锁——锁在手上了，再调 transactionTemplate.execute 开事务执行业务逻辑——execute 返回时事务已提交——finally 里再释放锁。整个流程是：拿锁 → 开事务 → 提交事务 → 释放锁，顺序严格保证。

---

**提问**：@Transactional 还有哪些容易失效的场景？

**回答**：
> 常见的有三个。一是 this 调用不生效——Service 内部用 this.xxx() 调另一个 @Transactional 方法时，this 是原始对象不走代理，事务不生效。必须通过注入的代理对象调。
>
> 二是非 public 方法不生效——Spring AOP 基于 JDK 动态代理或 CGLIB，只能代理 public 方法。
>
> 三是异常被 catch 了不回滚——@Transactional 默认只回滚 RuntimeException 和 Error。catch 了没抛出去，或者抛的是 checked exception 但没设 rollbackFor，都不会回滚。

---

## 八、RAG 智能客服

**知识点**：检索增强生成的全链路工程实现

**技术点**：
- RAG 四步：检索 → 增强 → 生成 → 校验
- 混合检索公式：`max(关键字分, 向量分 × 0.35) + 双命中加 5 分`
- 向量检索：MySQL 存向量 + 余弦相似度 ≥ 0.18 过滤
- 无资料拒绝：`documents.isEmpty()` → 不调 DeepSeek
- Normalizer：citations 白名单校验
- Embedding 切换：开发 hash(128维) / 生产 OpenAI(1536维)

**提问**：你做的智能客服和直接调 DeepSeek 有什么区别？

**回答**：
> 直接调的话模型不知道本社区的信息——维修电话多少、停水通知发了没、哪个车牌能进哪个门。我做了 RAG，就是在调模型之前先从知识库检索相关社区资料，拼进 Prompt 再让模型回答。
>
> 检索用了混合模式——关键字匹配精确但僵硬，向量语义灵活但可能不准。两路都搜，加权合并：`max(关键字分, 向量分 × 0.35)`，两边都命中再加 5 分奖励。向量存在 MySQL 里，用余弦相似度比对，低于 0.18 的当噪声丢掉。
>
> 检索不到资料时我不调 DeepSeek——省钱也安全——直接返回"请联系人工客服"。

---

**提问**：模型可能会编造引用来源，你怎么处理？

**回答**：
> 每次检索出来的资料都有 sourceId，Normalizer 维护了一个白名单。模型返回的回答里带了 citations，Normalizer 逐个检查——在白名单里就保留，不在就直接丢掉。
>
> 另外 confidence 做了限幅，模型不能随便写个 999。空字段也有兜底——answer 为空时补一段提示文案。Normalizer 的原则是只做防御性修正，不改模型输出的实质内容。

---

## 九、MySQL 索引与事务

**知识点**：关系型数据库核心机制

**技术点**：
- B+ 树：非叶子不存数据、叶子双向链表、范围查询高效
- 最左前缀：联合索引 (a,b,c)，断在中间的列不走索引
- 隔离级别：InnoDB 默认 RR，靠间隙锁解决幻读
- explain：type=ALL 是全表扫，要优化

**提问**：MySQL 为什么用 B+ 树不用 B 树？

**回答**：
> B+ 树的非叶子节点只存索引不存数据。同样大小的节点能装更多的索引项，树就更矮，IO 次数更少。而且叶子节点之间有双向链表串联，做范围查询比如 id BETWEEN 1 AND 100 时，找到起点顺着链表往后扫就行，不用回上层节点。B 树的叶子节点没有链表，范围查询要反复走树。

---

**提问**：联合索引 (a,b,c)，查 WHERE a=1 AND c=3 走索引吗？

**回答**：
> 只用到 a。因为最左前缀原则，b 在索引里排在 a 后面、c 前面。你跳过了 b 直接查 c，MySQL 没法跳过中间列直接用索引定位 c。explain 里 key_len 能验证——只把 a 的用上了，b 和 c 没用。所以建联合索引要按查询频率排序，没查到的列放最后。

---

## 十、Java 内存模型与并发

**知识点**：Java 并发编程基础

**技术点**：
- volatile：可见性 + 禁止指令重排，不保证原子性
- synchronized：监视器锁，JVM 层面自动加锁释放
- ReentrantLock：API 层面手动控制，支持公平锁、可中断、条件变量
- HashMap：1.7 数组+链表 / 1.8 数组+链表+红黑树（链表≥8 转树）
- 线程池：corePoolSize / maxPoolSize / keepAlive / 工作队列 / 拒绝策略

**提问**：volatile 和 synchronized 的区别？

**回答**：
> volatile 只保证两件事——可见性和禁止指令重排。一个线程改了 volatile 变量，其他线程立刻能读到。但它不保证原子性，比如 i++ 这种读-改-写三步操作，volatile 管不住。
>
> synchronized 三者都保证——可见性、原子性、有序性。但它更重，进出监视器有开销。如果只是简单的状态标记，用 volatile；如果是复合操作，用 synchronized 或者 ReentrantLock。

---

**提问**：HashMap 1.7 和 1.8 的区别？

**回答**：
> 1.7 是数组加链表，头插法。高并发下 resize 的时候链表可能成环，CPU 100%。1.8 改成尾插法，解决了死循环。另外 1.8 加了个优化——链表长度超过 8 且数组长度超过 64 时，链表转红黑树，查找从 O(n) 变成 O(log n)。但如果要线程安全还是用 ConcurrentHashMap，1.8 里用 CAS 加 synchronized 锁单个桶，性能比 1.7 的分段锁好。

---

## 十一、Spring 全家桶

**知识点**：Spring 核心机制

**技术点**：
- `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`
- 自动配置原理：`spring.factories` → 条件注解 `@ConditionalOnClass`/`@ConditionalOnMissingBean`
- Bean 生命周期：实例化 → 属性填充 → Aware → 后置处理器 → 初始化 → 销毁
- IoC / DI：控制反转和依赖注入的区别

**提问**：Spring Boot 自动配置原理是什么？

**回答**：
> `@SpringBootApplication` 里有 `@EnableAutoConfiguration`，它会去读 classpath 下所有 jar 包里的 `spring.factories` 文件。这个文件里注册了一堆 `xxxAutoConfiguration` 类。每个类上都有一堆条件注解——`@ConditionalOnClass` 判断 classpath 有没有这个类，`@ConditionalOnMissingBean` 判断容器里有没有用户自定义的 Bean。用户自己定义了就用用户的，没定义就用默认的。这就是"约定大于配置"的底层实现。
>
> 比如你引入了 `spring-boot-starter-data-redis`，自动配置类检测到 `RedisConnectionFactory` 在 classpath 上，就帮你创建一个 `RedisTemplate`。没引入就不创建。

---

## 十二、个人评价类问题

**知识点**：HR 面与综合素质

**提问**：为什么选 Java 后端？

**回答**：
> 大学学软件工程，课程里接触了 Java 之后开始自己写项目。做智慧社区的过程里，从 Spring Boot 搭一个接口，到分布式锁、缓存优化、消息队列，再到后来集成大模型做 AI 客服，整个过程让我觉得 Java 后端不只是 CRUD——它的生态和工程体系能支撑很复杂的需求。我想在这个方向上继续挖深。

---

**提问**：三年内的职业规划？

**回答**：
> 第一年把 Java 后端基础打扎实，能独立负责一个模块从设计到上线的全流程。第二年接触更大的分布式系统，同时继续跟 AI 工程化方向——不是做算法，是把模型推理能力落地成稳定可观测的服务。第三年希望自己能带一个小模块的技术方案设计。

---

**提问**：开发中遇到的最难的问题是什么？

**回答**：
> 最难的是分布式锁和事务的顺序问题。一开始按常规做法把锁放在 @Transactional 方法里面，表面上看代码没问题，但压测时偶尔出现重复计费。排查了好久才找到——锁在方法体里释放了，但 @Transactional 在方法返回后才真正提交事务。中间的窗口被其他线程钻了空子。
>
> 后来用 TransactionTemplate 把拿锁放到事务外面，先锁后事务，问题彻底解决。这个坑让我对 Spring 的事务代理机制理解深了很多，不再是"加个注解就行"。

---

**提问**：有在看什么技术吗？

**回答**：
> 最近在补数据结构与算法——因为面试必考，工程里的一些设计决策其实也是数据结构的选择，比如 B+ 树和跳表。另外在关注大模型应用的工程化落地，下一步计划用 Python FastAPI 做 AI 推理层，和 Java 业务层拆开，练一下跨语言微服务通信。
