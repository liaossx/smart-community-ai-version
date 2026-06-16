# Java 后端实习面试标准答案（44 题）

---

## 一、Java 基础

**知识点 1**：面向对象三大特性
**题**：封装、继承、多态分别是什么？多态怎么实现的？

把数据和操作打包在一起，对外只暴露必要的接口。比如 RedisLockUtil 外面只调 tryLock 和 unlock，里面是 SET NX 还是 Lua 脚本不需要知道。继承是子类复用父类代码，ActivityServiceImpl 继承 MyBatis-Plus 的 ServiceImpl，增删改查不用自己写。多态是同一个接口可以有多个实现类——CustomerServiceAssistant 现在用 RAG 客服实现，以后换另一个实现，Controller 不用改一行代码。多态底层靠方法表，JVM 根据对象实际类型找对应的方法执行。

**知识点 2**：String
**题**：String、StringBuilder、StringBuffer 的区别？String 为什么是不可变的？

String 底层是 final char 数组，每一次"修改"其实是 new 了一个新 String。如果循环里用 String 反复拼接，每次都会 new 一个对象，堆里留下大量垃圾。StringBuilder 解决了这个问题——直接在原对象内部修改，不 new 新对象，所以循环拼接要用它。StringBuffer 和 StringBuilder 功能一模一样，只是方法上加了 synchronized，多线程安全但更慢。String 设计成不可变是因为它到处被拿来做 key、做常量，不可变才最安全。

**知识点 3**：HashMap
**题**：HashMap 的底层结构？1.7 和 1.8 有什么变化？为什么链表长度到 8 转红黑树？

底层是数组 + 链表 + 红黑树。key 算 hash 对数组长度取模，找到对应位置。空着直接放，有东西就挂在链表后面。1.7 用的是头插法，新元素插在链表头部，并发扩容时链表可能成环导致死循环。1.8 改成尾插法，新元素挂在链表最后，解决了死循环问题。同时 1.8 还加了红黑树——链表长度到 8 且数组长度到 64 就自动转树。因为链表查找是 O(n)，8 个元素最坏比 8 次；红黑树 O(log n)，8 个元素最多比 3 次。8 是空间和时间的平衡点，出现概率只有千万分之六，一旦出现就说明哈希碰撞严重，转树防攻击。

**知识点 4**：ArrayList vs LinkedList
**题**：ArrayList 和 LinkedList 的区别？什么场景用哪个？

ArrayList 底层是数组，内存里连续一排，按索引查 O(1) 很快。但中间插入删除 O(n)——插入位置后面所有元素都要往后挪一格。LinkedList 底层是双向链表，内存里散着放，每个节点存前后指针。头尾插入删除 O(1) 只需改两根指针，不用挪其他元素。但按索引查询 O(n)——要从头顺着指针一个个数过去。日常开发 90% 用 ArrayList，只有频繁头尾增删的场景才用 LinkedList。而且 LinkedList 按索引插入要先找到位置，这个查找本身就是 O(n)。

**知识点 5**：异常
**题**：Exception 和 Error 的区别？RuntimeException 和 checked exception 有什么区别？

Exception 是代码逻辑或参数出问题，能用 try-catch 处理。项目里报名重复就 catch DuplicateKeyException 忽略。Error 是 JVM 或系统层面的问题，OOM、StackOverflow，代码处理不了。RuntimeException 编译器不强制 try-catch，比如 NullPointerException。checked exception 强制你 try-catch 或者 throws，比如读文件时 FileNotFoundException。Spring 框架大量用了 RuntimeException，让业务代码不被强制异常处理淹没。

**知识点 6**：反射
**题**：什么是反射？在项目中哪里用到了？

运行时动态拿到类的字段和方法去调用，不是编译时就写死。自己没直接写，但 Spring 到处在用它——@Autowired 就是反射拿到 private 字段后 setAccessible(true) 强行赋值；@Transactional 是反射扫到注解后在方法前后加了事务代码；MyBatis-Plus 的 Mapper 接口没有实现类，是反射动态生成了代理对象。

---

## 二、Spring 全家桶

**知识点 7**：IoC 和 DI
**题**：什么是控制反转和依赖注入？Spring 怎么管理 Bean 的？

IoC 控制反转是把对象创建权交给 Spring 容器，不再自己 new。DI 依赖注入是 Spring 自动把需要的 Bean 塞给你——写 @Autowired 就帮你注入。Spring 管理 Bean 分三步：启动时扫描包下的类，反射创建 Bean 放进一个 Map（IoC 容器），需要时按类型或名字从 Map 里取出来注入。

**知识点 8**：@Autowired 和 @Resource
**题**：@Autowired 和 @Resource 的区别？

@Autowired 是 Spring 的，按类型注入。同一个类型有多个实现时需要 @Qualifier 指定名字。@Resource 是 JDK 自带的，默认按名字注入，找不到再按类型。项目里全用 @Autowired，因为每个接口只有一个实现类，不需要区分名字。

**知识点 9**：Spring Boot 启动流程
**题**：@SpringBootApplication 里面包含了哪些注解？自动装配是怎么做的？

@SpringBootApplication 是一个组合注解，包含三个核心：@SpringBootConfiguration 标识配置类、@EnableAutoConfiguration 开启自动装配、@ComponentScan 扫描当前包和子包。自动装配的原理是 @EnableAutoConfiguration 去读所有 jar 包里的 META-INF/spring.factories 文件，里面注册了 xxxAutoConfiguration。每个类上有条件注解——@ConditionalOnClass 判断 classpath 有没有这个类，@ConditionalOnMissingBean 判断用户有没有自定义 Bean。用户定义了就用用户的，没定义才用默认的。

**知识点 10**：Bean 的生命周期
**题**：Spring Bean 的生命周期说几个关键步骤？Bean 是单例的吗？如何改成多例？

Bean 默认是单例的，整个容器只有一个实例。想改成多例就在类上加 @Scope("prototype")——每次 getBean 都 new 一个新的。关键步骤：实例化（new）→ 属性赋值（注入其他 Bean）→ BeanPostProcessor 前置处理 → @PostConstruct 初始化 → BeanPostProcessor 后置处理 → Bean 就绪 → 容器关闭时 @PreDestroy 销毁。

**知识点 11**：@Transactional
**题**：@Transactional 失效的场景有哪些？事务传播行为有哪些？

三种常见失效：this 调用不走 AOP 代理；非 public 方法 AOP 拦不到；异常被 catch 没抛出去，Spring 感知不到。事务传播行为常用两个：REQUIRED（默认）——有事务就加入，没有就新建；REQUIRES_NEW——不管外面有没有事务，自己开新的。项目里出闸接口因为锁和事务的顺序问题改成了 TransactionTemplate，本质上就是把事务控制权从注解拿回来自己管。

---

## 三、MySQL

**知识点 12**：索引数据结构
**题**：MySQL 索引底层为什么用 B+ 树？和二叉树、Hash 比有什么好处？

二叉树如果是递增插入会退化成链表。Hash 等值查询 O(1) 很快，但不支持范围查询——查 id 在 1 到 100 之间，Hash 索引用不了。B+ 树能自平衡不会退化，非叶子节点只存索引所以同样大小的节点能装更多索引项，树更矮，IO 次数更少。叶子节点之间有双向链表串着，范围查询时找到起点顺着链表往后扫就行，不用回上层节点。

**知识点 13**：聚簇索引和二级索引
**题**：聚簇索引和二级索引的区别？什么是回表？

聚簇索引就是主键索引，B+ 树的叶子节点直接存整行数据。二级索引（普通索引、唯一索引、联合索引）的叶子节点只存索引列的值 + 主键 ID。查 SELECT * 的时候，二级索引先拿到主键 ID，再回聚簇索引查完整行——"回表"。覆盖索引就是把查询需要的字段全塞进一个联合索引里，不用回表。

**知识点 14**：最左前缀
**题**：联合索引 (a, b, c)，WHERE b=1 AND c=2 走索引吗？为什么？

不走。联合索引必须从最左边的列开始连续匹配。WHERE b=1 跳过了 a，断了。WHERE a=1 AND c=3 只用到 a——c 被跳过了中间的 b。建联合索引时把查询频率高、等值比较的列放前面，范围查询的列放最后。

**知识点 15**：索引失效
**题**：索引失效的场景有哪些？LIKE "%xxx" 为什么不走索引？

最左前缀断了、索引列做运算（WHERE age+1=20）、索引列用函数（DATE(create_time)）、LIKE 以 % 开头（不知道前缀是什么，B+ 树没法定位）、类型隐式转换（VARCHAR 字段传了数字）、OR 条件里不是所有列都有索引。

**知识点 16**：事务隔离级别
**题**：MySQL 默认的事务隔离级别是哪个？什么是幻读？怎么解决的？

InnoDB 默认是可重复读。幻读是同一个事务里两次范围查询之间，别人插入新行导致结果多了一行。解决：快照读靠 MVCC——每个事务看到的是自己开始时的数据版本。当前读（SELECT FOR UPDATE）靠 Next-key Lock——锁住行 + 行之间的间隙，别人插不进来。项目里没改默认级别，报名并发用 Redis + MQ 提前扛住了。

**知识点 17**：SQL 优化
**题**：explain 怎么看？type 有哪些？什么情况是全表扫描？

先看 type——ALL 是全表扫必须优化，ref 和 const 是好的。再看 key——看实际用了哪个索引，NULL 表示没走索引。rows 预估扫描行数越小越好。Extra 里 Using index 表示覆盖索引不用回表，最优；Using filesort 表示排序没用上索引，要优化；Using temporary 表示用了临时表，更差。

**知识点 18**：分页优化
**题**：深分页 LIMIT 100000,10 为什么慢？怎么优化？

MySQL 会先扫前面 100000 行再扔掉，全表扫。优化方式一：记住上一页最后一个 id，WHERE id > 上次最后一个 LIMIT 10。方式二：延迟关联——子查询只拿主键 ID 走覆盖索引，外层 JOIN 拿完整行。

---

## 四、Redis

**知识点 19**：缓存穿透 / 击穿 / 雪崩
**题**：缓存穿透、击穿、雪崩分别是什么？怎么解决？

穿透：查不存在的数据，缓存和 DB 都没有。解决：缓存空对象设短 TTL，或用布隆过滤器前置拦截。击穿：热点 key 过期，大量请求同时打 DB。解决：定时预热。项目里车位余量每 5 分钟预热一次，用户第一次查就是 3ms。雪崩：大量 key 同时过期。解决：不同缓存设不同 TTL——parkingRemain 10s、mySpaces 30s、houseInfo 5min。

**知识点 20**：数据结构
**题**：Redis 有哪几种常用数据结构？你项目里用了哪些？

String（缓存车位余量）、List、Set（防重复报名）、Hash、ZSet。项目里 Redis 干了三件事：缓存车位余量、分布式锁控制停车出闸幂等、Set 防止同一个用户重复报名。

**知识点 21**：分布式锁
**题**：怎么用 Redis 实现分布式锁？SET NX 是什么意思？锁过期了怎么办？

SET key value NX EX 10。NX 表示 key 不存在才创建，EX 10 表示 10 秒后自动过期。拿锁失败就重试——项目里重试 5 次每次间隔 200ms。释放用 Lua 脚本：先 GET 比对自己的 value，是自己的才 DEL，防止误删别人的锁。锁超时方案：Redisson Watch Dog 自动续期。项目里出闸业务几百毫秒就完成，10 秒的超时完全够用。

**知识点 22**：持久化
**题**：RDB 和 AOF 的区别？生产环境两个都开吗？

RDB 是定时快照，文件小恢复快，但两个快照之间挂掉会丢数据。AOF 是写命令日志，每条写操作都记录，更安全但文件大恢复慢。生产环境两个都开——RDB 做主备份快速恢复，AOF 补增量的数据安全。项目里没开持久化——缓存丢了下次查 MySQL 重建，分布式锁丢了用户重试即可。

**知识点 23**：缓存一致性
**题**：先删缓存再更新数据库，还是先更新数据库再删缓存？为什么？

先更新 DB 再删缓存。如果先删缓存再写 DB——删完缓存后、写 DB 前这个窗口，别人读到旧数据又写回了缓存，这个缓存里的脏数据永远不会被清理。反过来——写完 DB 后删缓存，最多有一个极短的窗口别人读到旧缓存，但下一次 DB 更新后会再次清掉。项目里 @CacheEvict 在事务提交后才执行，这个窗口被收得更窄。

---

## 五、消息队列（RabbitMQ）

**知识点 24**：消息可靠性
**题**：RabbitMQ 怎么保证消息不丢失？

三段防护：发送端开启 publisher confirm（确保 MQ 收到了消息）；MQ 本身队列和消息设为持久化（写到硬盘防宕机丢）；消费端手动 ack（处理成功才确认，失败不确认会重新投递）。三段全开。

**知识点 25**：重复消费
**题**：消息重复消费了怎么办？你怎么处理的？

消费端做幂等。项目里活动报名表有唯一索引 uk_activity_user(activity_id, user_id)，重复 insert 会抛 DuplicateKeyException，catch 住直接忽略，不会重复写一条报名记录。再多一个兜底：Redis 的 Set 层已经拦了重复报名，正常情况消息根本不会重复投。

**知识点 26**：使用场景
**题**：什么场景用消息队列？你项目里怎么用的？

削峰、异步、解耦。项目里活动报名——Lua 扣完 Redis 库存后发 MQ，消费端异步写 MySQL。用户不感知 DB 写入的耗时，消费端慢慢处理。MQ 发不出去有 publisher confirm 兜底，消息丢了有定时对账任务做最终校准。

**知识点 27**：选型
**题**：RabbitMQ 和 Kafka 有什么区别？什么时候用哪个？

RabbitMQ 是消息队列，消费完消息就没了。Kafka 是分布式日志，消息可以回溯重放。Kafka 靠硬盘顺序写吞吐极高（百万级/秒），适合大数据管道。RabbitMQ 轻量、部署简单、文档多，适合业务消息投递。项目里一天消息不到万级，RabbitMQ 完全够用。

---

## 六、微服务

**知识点 28**：Nacos 的作用
**题**：Nacos 是干什么的？注册中心和配置中心分别解决什么问题？

两个身份：注册中心——服务启动时把自己注册上去，其他服务通过服务名而不是 IP 来调用；配置中心——核心配置全托管在 Nacos 上，改了配置自动推送不重启。项目里 core-service.yaml 管着 MySQL/Redis/RabbitMQ 配置，所有微服务共享一份。

**知识点 29**：Gateway
**题**：Spring Cloud Gateway 是干什么的？和 Nginx 有什么区别？

Gateway 是微服务体系的统一入口。请求进来它负责路由转发（/api/parking/ 转到 parking-service）、限流（集成 Sentinel）、鉴权。Nginx 是通用反向代理，配置简单，通常放最外层做负载均衡和静态资源。Gateway 是 Spring Cloud 原生组件，和 Nacos 集成更深。

**知识点 30**：Sentinel
**题**：Sentinel 是干什么的？限流和熔断有什么区别？

Sentinel 是流量控制。限流是流量大了拒绝——防别人打你。熔断是下游挂了就断开——保护自己不被拖垮。项目里在 Gateway 层做了接口 QPS 限流：AI 服务 50 QPS，停车服务 200 QPS。规则存在 Nacos 上，改了不重启。被限流时直接返回 429 "请求过于频繁"。

**知识点 31**：Feign / OpenFeign
**题**：Feign 的原理是什么？一个接口加 @FeignClient 为什么就能调用了？

Feign 是一个声明式 HTTP 客户端。写一个接口加 @FeignClient("service-name")，底层用动态代理——运行时把接口方法调用翻译成 HTTP 请求，从 Nacos 查到目标服务的地址，发请求过去，拿到 JSON 反序列化成返回值。项目里 system-service 调 property-service 就是走 Feign。

---

## 七、JVM

**知识点 32**：堆结构
**题**：JVM 内存模型大概讲一下？年轻代和老年代有什么区别？

堆最大，放所有 new 出来的对象。堆内分两块：年轻代放短命对象（请求里的 DTO、字符串拼接，方法结束就死）；老年代放长命对象（Spring Bean、连接池，活到项目关闭）。年轻代的 GC（Minor GC）频繁但快十几毫秒，老年代的 GC（Full GC）偶尔但慢几百毫秒。堆外还有方法区（放 static 变量和 class 图纸）和栈（放局部变量）。

**知识点 33**：GC
**题**：Minor GC 和 Full GC 的区别？你项目怎么配 GC 参数的？

Minor GC 清年轻代，频繁快；Full GC 清老年代，偶尔慢。项目 Java 17 默认用 G1——分块收集，只清最脏的块，停顿可控。启动参数：-Xms256m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200。加 GC 日志和 OOM 自动 dump，方便排查。

**知识点 34**：OOM
**题**：OOM 是什么？你项目中出现过吗？排查思路是什么？

堆满了。原因：线程池无界队列任务排了几万个；ThreadLocal 用完没 remove 旧数据堆积；集合里塞了对象忘了清。排查：启动参数加 HeapDumpOnOutOfMemoryError 让崩时自动导出 dump 文件，MAT 打开看 Dominator Tree，谁占最多追谁。

**知识点 35**：栈和堆
**题**：堆和栈的区别？什么变量在栈上，什么在堆上？

堆放 new 出来的对象，所有人共用，GC 管。栈放局部变量和方法调用链，线程私有，方法结束自动清。比如 String lockKey 这个变量在栈上，"lock:parking:exit:粤B12345" 这个字符串对象在堆里。static 变量在方法区，不在堆也不在栈。

---

## 八、计算机网络

**知识点 36**：HTTP 和 HTTPS
**题**：HTTP 和 HTTPS 的区别？HTTPS 怎么保证安全的？

HTTP 明文传输，抓包能看到密码。HTTPS 在 TCP 和 HTTP 之间加了 SSL/TLS 层。过程：客户端请求 → 服务端返回证书 → 客户端验证证书真伪 → 双方协商对称加密密钥 → 后续通信全加密。用的是非对称加密交换密钥，对称加密传输数据。

**知识点 37**：TCP 三次握手
**题**：TCP 为什么是三次握手，不是两次不是四次？

两次不可靠——客户端发 SYN 但网络延迟很久才到，服务端以为新请求白白开着连接。三次确认双方收发都正常：第一次客户端发 SYN（我能发吗），第二次服务端回 SYN+ACK（我能收发，你呢），第三次客户端回 ACK（我也能收发，开干）。四次多余，三次刚好。

**知识点 38**：GET vs POST
**题**：GET 和 POST 的区别？POST 是安全的吗？

GET 参数在 URL 上，有长度限制，可缓存可收藏，幂等。POST 参数在请求体里，无长度限制，不幂等，每次可能产生不同结果。POST 不保证安全——HTTPS 加密后请求体也是加密的，比明文安全，但不是说 POST 本身是安全协议。

**知识点 39**：Cookie 和 Session
**题**：Cookie 和 Session 的区别？JWT 跟它们有什么关系？

Cookie 存浏览器端，每次请求自动带上。Session 存服务端，Cookie 里只放 Session ID。JWT 替代了 Session——用户信息编码加密后存在客户端，服务端不存状态。每次请求带 Token 验签名还原用户信息。项目里用 JWT：过滤器从 header 拿 Token 解析出 userId 和 role，塞进 UserContext 方便全链路取用。

---

## 九、简历必问（从你项目里出）

**知识点 40**：项目难点
**题**：项目里遇到的最难的技术问题是什么？怎么解决的？

分布式锁和事务的顺序问题。锁放在 @Transactional 方法里，锁释放了但事务还没提交，其他线程拿到锁读到旧数据，停车场重复计费。排查后才定位到是 AOP 代理和锁释放的顺序问题。改用 TransactionTemplate——先拿锁、再开事务、事务提交了才释放锁，顺序严格保证，问题解决。

**知识点 41**：项目亮点
**题**：你觉得你项目里最能拿出手的一个亮点是什么？

RAG 智能客服模块。做了混合检索（关键字 + 向量语义两路搜、加权合并），Prompt 拼装，DeepSeek 调用，Normalizer 输出校验——citations 白名单，模型的回答可追溯可验证。还做了无资料拒绝机制——检索不到资料不调大模型，既省钱又安全。加上可观测性日志，每次 AI 调用的耗时和成本都有记录。

**知识点 42**：技术选型
**题**：你的项目为什么用 RabbitMQ 而不是 RocketMQ？为什么用 Nacos 而不是 Eureka？

社区几千户人，一天消息量不到万级，RabbitMQ 的万级吞吐完全够用。RocketMQ 支持事务消息但需要更大的部署资源。Eureka 已经停止维护，Nacos 同时做注册和配置中心，一个组件干两件事，运维成本更低。选型是选复杂度匹配当前风险的，不是选最强的。

---

## 十、算法（小厂基本不考，但准备两道防身）

**知识点 43**：排序
**题**：手撕快排 / 归并排序

快排：选基准数，比它小的放左边，大的放右边，左右递归。平均 O(n log n)，最坏 O(n^2)。归并：分半拆成最小单元，两两合并排序。总是 O(n log n)，但需要额外空间。快排用得更多——Java 的 Arrays.sort() 底层就是对基本类型用快排，对对象类型用归并。

**知识点 44**：LRU
**题**：LRU 缓存怎么实现？（用 LinkedHashMap 一句话）

```java
Map<String, Object> cache = new LinkedHashMap<>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > 100;
    }
};
```

LinkedHashMap 的 accessOrder=true 时，每次 get 或 put 都会把该节点移到链表头部。链表尾部的就是最久没访问过的。重写 removeEldestEntry——当 size 超过 100 时自动淘汰尾部节点。这就是 LRU 的标准实现。
