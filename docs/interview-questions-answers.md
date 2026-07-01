# 智慧社区项目 — 面试题答案（8 题）

---

## 第 1 题：Redis + Lua 脚本原子预扣库存

**题目**：请详细说明在智慧社区项目中，你使用 Redis + Lua 脚本原子预扣库存的具体实现过程。如何确保在高并发下库存扣减的正确性和性能？如果 Redis 宕机，你如何保证数据不丢失？

### 1.1 为什么用 Lua 脚本

活动报名在高并发下如果用传统方式（`GET` → 判断 → `DECR` → `SADD`），需要 4~6 次 Redis 网络往返，且不是原子操作——两个请求同时读到 stock=1，都判断 > 0，都执行 DECR，库存变成 -1，超卖。

Lua 脚本在 Redis 单线程中执行，整个脚本不会被其他命令插入。一次 EVAL 搞定所有操作，网络往返从 4~6 次降为 1 次。

### 1.2 Lua 脚本四步原子操作

代码位置：`community-service/.../ActivityServiceImpl.java:39-66`

```
KEYS[1] = "activity:stock:{id}"    -- 库存 key
KEYS[2] = "activity:users:{id}"    -- 已报名用户 Set
ARGV[1] = userId                   -- 当前用户 ID
ARGV[2] = initialStock             -- MySQL 算好的初始库存 (maxCount - signupCount)
```

| 步骤 | Redis 命令 | 逻辑 |
|------|-----------|------|
| ① 查重 | `SISMEMBER userSetKey userId` | 已报名则返回 `{0, 'DUPLICATE'}` |
| ② 懒初始化 | `EXISTS stockKey` → `SET` | 首次访问用 MySQL 剩余名额初始化（Lua 不能访问 MySQL，由 Java 层传入） |
| ③ 原子扣减 | `DECR stockKey` | 扣 1；若结果 < 0 立即 `INCR` 回滚，返回 `{0, 'SOLD_OUT'}` |
| ④ 标记用户 | `SADD userSetKey userId` | 记录已报名，返回 `{1, 'OK'}` |

### 1.3 高并发正确性 — 三层防护

**第一层：Redis Lua 原子性。** 整个脚本在单线程中执行，步骤 ③ 的 DECR + INCR 回滚是即时的，不存在"检查后到扣减前被插队"的窗口。

**第二层：DB 唯一索引幂等。** 代码位置：`ActivitySignupListener.java:38-43`。表 `sys_activity_signup` 有联合唯一索引 `uk_activity_user(activity_id, user_id)`。MQ 消费者 insert 时如果消息重复，`DuplicateKeyException` 被 catch 忽略。

**第三层：原子 SQL 更新报名数。** 代码位置：`ActivitySignupListener.java:46-51`。`SET signup_count = IFNULL(signup_count, 0) + 1` 是 DB 层面的原子操作，避免 "读-改-写" 的 race condition。

### 1.4 高性能设计

- **单次 EVAL 替代 4~6 次网络 I/O**：原方案需要 GET + SETNX + DECR + INCR(回滚) + SADD，至少 4 次往返，Lua 合并为 1 次。
- **异步写库**：Redis 扣减成功后立即返回，落 MySQL 通过 RabbitMQ 异步完成（`ActivityServiceImpl.java:197-204`），用户不感知 DB 写入耗时。
- **懒初始化**：库存 key 不需要提前预热，首次报名时 Lua 自动初始化。

### 1.5 Redis 宕机 — 数据不丢失的三层保障

**保障一：AOF 持久化。** Redis 配置 AOF + appendfsync everysec，最多丢 1 秒数据。重启后回放 AOF 恢复库存 key 和用户 Set。

**保障二：定时对账任务。** 代码位置：`ActivityStockReconciliationTask.java`。每 10 分钟 cron 扫描所有 ONLINE/PUBLISHED 的活动，对比 Redis 库存与 MySQL `signup_count`，**以 MySQL 为准修复 Redis**。覆盖了 Redis 数据完全丢失、key 过期等场景。

**保障三：DB 唯一索引兜底。** Redis 宕机期间即使有重复报名请求穿透到 MQ 消费者，`uk_activity_user` 唯一索引保证不会重复落库。

### 回答要点（面试官想听的）

> 不是背方案，是推导过程：**正常路径 → 并发冲突 → 依赖故障 → 数据一致性**。顺着这个链条讲：Lua 原子操作 → MQ 异步 → 唯一索引幂等 → 定时对账修复。

---

## 第 2 题：Spring Cache + Redis 缓存策略

**题目**：在项目中你提到使用 Spring Cache + Redis 将接口响应时间从 292ms 降至 3ms。请解释 Spring Cache 的工作原理，以及你如何设计缓存策略（如过期时间、缓存穿透、缓存雪崩的应对）？

### 2.1 Spring Cache 工作原理

Spring Cache 本质是 **AOP + CacheManager 抽象**：

1. `@EnableCaching` 开启后，Spring 扫描所有 `@Cacheable` / `@CacheEvict` / `@CachePut` 注解的方法
2. 生成 AOP 代理，方法调用前先拦截
3. `@Cacheable`：先查缓存，命中直接返回（不执行方法体）；未命中执行方法，结果写入缓存后返回
4. `@CacheEvict`：方法执行后清除指定缓存
5. CacheManager 是抽象层 — 我们用的是 `RedisCacheManager`（底层 Redis），但注解代码不感知具体实现

### 2.2 项目缓存配置

代码位置：`common-module/.../config/RedisCacheConfig.java`

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        // 支持 LocalDateTime 等 Java 8 时间类型
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(om);

        // 默认 30 秒 TTL
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(serializer)
                .disableCachingNullValues()  // 不缓存 null，防穿透
                .entryTtl(Duration.ofSeconds(30));

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("adminStatsOverview", baseConfig.entryTtl(Duration.ofSeconds(120)));
        configs.put("houseInfo", baseConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("communityInfo", baseConfig.entryTtl(Duration.ofMinutes(10)));
        configs.put("userDetail", baseConfig.entryTtl(Duration.ofMinutes(2)));
        configs.put("userList", baseConfig.entryTtl(Duration.ofMinutes(1)));
        configs.put("parkingRemain", baseConfig.entryTtl(Duration.ofSeconds(10)));
        configs.put("mySpaces", baseConfig.entryTtl(Duration.ofSeconds(30)));
        configs.put("noticeUnreadCount", baseConfig.entryTtl(Duration.ofSeconds(10)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(configs)
                .transactionAware()  // 事务提交后才执行 @CacheEvict
                .build();
    }
}
```

### 2.3 缓存策略设计

#### TTL 分级策略（防雪崩）

不同业务设不同 TTL，避免大量 key 同时过期造成雪崩：

| 缓存名 | TTL | 原因 |
|--------|-----|------|
| `parkingRemain` | 10s | 余位实时性高，短 TTL 保证准确 |
| `mySpaces` | 30s | 用户车位绑定不常变 |
| `noticeUnreadCount` | 10s | 通知计数更新频繁 |
| `userList` | 1min | 用户列表分页，可接受分钟级延迟 |
| `userDetail` | 2min | 用户详情不常变 |
| `houseInfo` | 5min | 房屋信息变更极少 |
| `communityInfo` | 10min | 社区信息基本不变 |
| `adminStatsOverview` | 120s | 管理后台统计不需要实时 |

#### 缓存穿透

`disableCachingNullValues()` — 不缓存 null 值，避免大量查询不存在的数据占用缓存空间。对于热点空值场景，业务代码在查询到空结果时返回空集合（而非 null），数据库不会被大量不存在 key 击穿。

#### 缓存击穿（热点 key 过期）

车位余量 `parkingRemain` 是典型热点。项目通过**定时预热**解决：定时任务每 5 分钟调一次 Service 方法，触发 `@Cacheable` 自动写缓存，保证热点 key 永不过期。

#### 缓存雪崩

① TTL 分级（如上表），不同缓存不同过期时间；② `transactionAware()` — 写操作的事务提交后才执行 `@CacheEvict`，最大程度收窄"清缓存和提交事务之间"的脏读窗口。

#### 缓存一致性

采用 **先写 DB 再删缓存** 策略：

- 写操作标注 `@CacheEvict(allEntries = true)`，数据变更后清空整个 cache
- `transactionAware()` 保证 @CacheEvict 在事务提交后才执行
- 极端脏读窗口（清缓存和提交事务之间）仅毫秒级，对车位余量这类秒级延迟可接受的场景完全够用

### 2.4 292ms → 3ms 的案例

以 `parkingRemain` 为例：

```
无缓存：请求 → ParkingSpaceServiceImpl.getRemaining()
         → MyBatis: SELECT COUNT(*) FROM biz_parking_space WHERE status='IDLE'
         → MySQL 扫描索引 → 返回
         网络 + SQL + 序列化 ≈ 292ms

有缓存：请求 → AOP 拦截 → Redis GET parkingRemain::all
         → 命中 → 反序列化 → 返回
         纯内存操作 ≈ 3ms
```

---

## 第 3 题：MySQL vs Oracle 对比

**题目**：JD 要求熟悉 MSSQL 和 Oracle 数据库，你的项目中主要使用 MySQL。请对比 MySQL 和 Oracle 在事务隔离级别、锁机制和 SQL 优化方面的主要差异，并举例说明如何将 MySQL 的优化经验迁移到 Oracle？

### 3.1 事务隔离级别

| 维度 | MySQL (InnoDB) | Oracle |
|------|---------------|--------|
| 默认级别 | **可重复读** | **读已提交** |
| 实现机制 | MVCC + Undo Log，事务看到快照版本 | MVCC + Undo Tablespace，通过 SCN 保证一致性 |
| 幻读解决 | Next-key Lock（行锁 + 间隙锁） | 多版本 + 语句级读一致性，天然不幻读 |
| 读已提交行为 | 每次 SELECT 生成新 ReadView | 语句开始时确定 SCN，语句内一致 |

核心差异：Oracle 的读已提交级别天然防止了"脏读"和"不可重复读"，但允许"幻读"（实际通过多版本基本避免了）。MySQL 的默认 RR 级别需要 Next-key Lock 来防幻读，锁粒度更重。

**迁移经验**：如果项目迁移到 Oracle，MySQL 中使用 RR + Gap Lock 的场景在 Oracle 中可以简化——Oracle 的读已提交 + 多版本基本等价于 MySQL RR 的效果，并发性能反而更好。但对 `SELECT FOR UPDATE` 场景要注意：Oracle 没有 Gap Lock，高并发下需要用 `SELECT ... FOR UPDATE WAIT n` 或 `SKIP LOCKED` 来避免锁等待。

### 3.2 锁机制

| 维度 | MySQL (InnoDB) | Oracle |
|------|---------------|--------|
| 锁粒度 | 行锁（索引记录锁）+ 间隙锁 + Next-key Lock | 行锁（TX 锁），无间隙锁 |
| 意向锁 | IS/IX 表级意向锁 | 无显式意向锁 |
| 死锁检测 | `innodb_deadlock_detect` | 自动检测，回滚代价小的事务 |
| 锁升级 | 不会锁升级（行锁占用内存） | 不会锁升级 |
| 阻塞行为 | 默认阻塞等待 | `SELECT FOR UPDATE` 默认阻塞；可用 `NOWAIT` / `SKIP LOCKED` |

关键差异：Oracle 没有 Gap Lock。在 MySQL 中用 `SELECT FOR UPDATE` 防幻读的场景，到 Oracle 不需要额外处理——Oracle 的读提交已经保证语句级一致性。但这也意味着 Oracle 在极端并发写入时更可能出现唯一键冲突，需要应用层处理。

**项目中对应**：`ActivitySignupListener.java` 中用 `DuplicateKeyException` 做幂等——这套"先 insert，catch 唯一键冲突"的模式在 Oracle 上完全适用，甚至更适合（Oracle 读提交级别下，唯一约束检查是即时的）。

### 3.3 SQL 优化对比

| 维度 | MySQL | Oracle |
|------|-------|--------|
| 优化器 | 基于成本 (CBO)，较简单 | 基于成本 (CBO)，更成熟强大 |
| 执行计划 | `EXPLAIN` | `EXPLAIN PLAN` + `DBMS_XPLAN.DISPLAY` |
| 索引类型 | B+ Tree（聚簇/二级）、全文索引 | B+ Tree、Bitmap、Function-based、反向键索引 |
| 分页 | `LIMIT offset, count` | `ROWNUM` 或 `OFFSET ... FETCH` (12c+) |
| 绑定变量 | 默认字面量，需手动用 `?` | 强制使用绑定变量（共享池复用） |
| Hint | 支持较少 | 丰富的 Optimizer Hint |

### 3.4 从 MySQL 迁移到 Oracle 的经验映射

**场景 1：深分页优化**

```sql
-- MySQL：记住上次 id
SELECT * FROM sys_fee WHERE id > 12345 ORDER BY id LIMIT 20;

-- Oracle：用 ROW_NUMBER() 或 FETCH
SELECT * FROM sys_fee WHERE id > 12345 ORDER BY id FETCH NEXT 20 ROWS ONLY;
```

**场景 2：覆盖索引 → Oracle 索引组织表 (IOT)**

项目中 MySQL 用联合索引做覆盖索引避免回表（如 `idx_fee_id` 只查 `fee_id` 时）。Oracle 可以用 IOT 把整张表按主键存储，二级索引直接指向物理地址。

**场景 3：explain 检查 → Oracle 执行计划分析**

MySQL 的 `EXPLAIN SELECT ...` 看 type/rows/Extra，对应 Oracle 用：
```sql
EXPLAIN PLAN FOR SELECT ...;
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```
关注 `TABLE ACCESS FULL`（全表扫）→ 需要建索引；`INDEX RANGE SCAN` → 优化器正确用了索引。

**场景 4：事务隔离级别的调整**

项目里 `ActivitySignupListener` 在 MySQL 的 RR 隔离级别下运行正常。到 Oracle 默认 RC 级别下，`INSERT ... ON DUPLICATE KEY` 可以改用 `MERGE INTO`，配合唯一约束同样达到幂等效果。

---

## 第 4 题：Hibernate vs MyBatis 对比

**题目**：JD 中提到了 Hibernate，而你的项目中使用了 MyBatis-Plus。请比较 Hibernate 和 MyBatis 在 ORM 设计理念上的主要区别，并讨论在 ERP 系统中，哪种框架更适合复杂报表查询和性能调优？

### 4.1 设计理念的根本区别

| 维度 | Hibernate (JPA) | MyBatis / MyBatis-Plus |
|------|----------------|----------------------|
| 核心理念 | **全自动 ORM** — 对象模型驱动，自动生成 SQL | **半自动** — SQL 为核心，手动编写或代码生成 |
| 映射方式 | 注解/XML 声明实体关系 → 自动 DDL + SQL | 手动写 SQL 或用 BaseMapper 自动 CRUD |
| 对象状态管理 | 持久化上下文 (Session/EntityManager)，自动脏检查、级联 | 无状态管理，每次操作即时执行 |
| 多表查询 | HQL/JPQL/Criteria API，对象导航 | 手写 SQL 或 MyBatis-Plus 的 Wrapper |
| 缓存 | 一级缓存（Session）+ 二级缓存（可插拔） | 无内置缓存，一般搭配 Spring Cache |
| 学习曲线 | 陡峭（懒加载、N+1、session 管理） | 平缓（会写 SQL 就能上手） |
| SQL 可控性 | 低 — Hibernate 生成的 SQL 不可预知、难调优 | 高 — 开发者完全控制每条 SQL |

### 4.2 项目中的选型理由

智慧社区项目选 MyBatis-Plus 的原因：

1. **复杂查询多**：缴费模块要跨 `sys_fee` / `sys_house` / `sys_user` 多表关联，管理后台列表有动态筛选（状态、用户名、社区），MyBatis 的 XML 或 Lambda Wrapper 比 HQL 更直观
2. **性能调优需要**：车位余量 `SELECT COUNT(*)` 需要精确控制 SQL 执行计划，MyBatis 手写 SQL 直接调，Hibernate 生成的 SQL 不可控
3. **学习成本**：团队时间紧，MyBatis-Plus 的 `BaseMapper<T>` 继承就能用增删改查，不用管 session/entityManager 生命周期

### 4.3 ERP 系统中复杂报表查询的框架选择

**MyBatis 更适合 ERP 报表查询**，原因：

1. **ERP 报表 = 复杂 SQL**。一张报表可能涉及 5~10 张表 JOIN + 分组 + 聚合 + 子查询，HQL 写不出来或者写出来很难读，手写 SQL 反而最直接。

2. **性能调优灵活**。ERP 系统查询量大，经常需要针对特定 SQL 加 hint、调 join 顺序、用数据库特有函数（`GROUP_CONCAT` / `ROW_NUMBER() OVER()`）。MyBatis 直接把 SQL 暴露出来，DBA 帮忙调优很方便；Hibernate 生成的 SQL 像一个黑盒。

3. **存储过程/函数调用**。ERP 系统大量使用存储过程做复杂计算，MyBatis 直接用 `CALL procedure_name()` 处理，Hibernate 虽然支持 `@NamedStoredProcedureQuery` 但配置繁琐。

4. **Hibernate 更适合什么**：CRUD 密集、对象关系复杂（1:N, M:N 级联）、领域模型驱动的增删改。如果是一个电商订单系统（用户 → 订单 → 订单明细的级联创建），Hibernate 的级联保存和脏检查确实方便。但 ERP 的核心是"查"而不是"写"，MyBatis 更对路。

### 4.4 "MyBatis 经验如何迁移到 Hibernate"

如果团队规定用 Hibernate，可以做以下平衡：

- 简单的 CRUD 用 Spring Data JPA（`JpaRepository`），增删改自动生成
- 复杂查询用 `@Query(nativeQuery = true)` 写原生 SQL，等同于 MyBatis 的手写 SQL 能力
- 报表查询用 `JdbcTemplate` 直接操作，不走 ORM

---

## 第 5 题：物业缴费模块数据库设计

**题目**：在智慧社区项目中，你独立完成了从数据库设计到接口开发的全流程。请具体描述你如何设计物业缴费模块的数据库表结构，包括表关系、索引设计和分表策略？

### 5.1 业务场景分析

物业缴费的核心流程：

- 管理员按社区/楼栋/周期批量生成账单（面积 × 单价）
- 业主查看待缴账单 → 支付 → 回调确认
- 管理员催缴（发系统通知）
- 业主查看缴费历史记录

抽象出的核心实体：**账单 (Fee)** 和 **缴费记录 (FeeRecord)**。

### 5.2 表结构设计

#### sys_fee（物业费账单表）

```sql
CREATE TABLE sys_fee (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    house_id      BIGINT NOT NULL COMMENT '房屋ID',
    community_id  BIGINT NULL COMMENT '社区ID',
    building_no   VARCHAR(20) COMMENT '楼栋号',
    fee_cycle     VARCHAR(20) NOT NULL COMMENT '费用周期(2025-01)',
    fee_amount    DECIMAL(10,2) NOT NULL COMMENT '金额',
    fee_type      VARCHAR(20) DEFAULT '物业费',
    status        VARCHAR(20) DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/OVERDUE',
    remind_count  INT DEFAULT 0 COMMENT '催缴次数',
    due_date      DATETIME COMMENT '截止时间',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    remark        VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='物业费账单';
```

#### sys_fee_record（缴费记录表）

```sql
CREATE TABLE sys_fee_record (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    fee_id        BIGINT NOT NULL COMMENT '账单ID',
    user_id       BIGINT NOT NULL COMMENT '缴费人ID',
    house_id      BIGINT NOT NULL COMMENT '房屋ID',
    pay_amount    DECIMAL(10,2) NOT NULL COMMENT '实际缴费金额',
    pay_type      VARCHAR(20) NOT NULL COMMENT 'WECHAT/ALIPAY/CASH',
    pay_time      DATETIME COMMENT '缴费时间',
    order_no      VARCHAR(64) COMMENT '支付订单号',
    trade_no      VARCHAR(64) COMMENT '交易流水号',
    status        VARCHAR(20) DEFAULT 'FAIL' COMMENT 'SUCCESS/FAIL/REFUND',
    remark        VARCHAR(255),
    PRIMARY KEY (id),
    INDEX idx_fee_id (fee_id),
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB COMMENT='缴费记录表';
```

### 5.3 表关系

```
sys_house (房屋)                    sys_user (用户)
    │ 1                               │ 1
    │ has many                        │ has many
    ↓ N                               ↓ N
sys_fee (账单)    ←─ 1:1 支付 ─→  sys_fee_record (缴费记录)
  - house_id                          - fee_id
  - community_id                      - user_id
  - fee_cycle                         - house_id
  - fee_amount                        - pay_amount
  - status                            - pay_type
                                      - order_no
```

- `sys_fee` 1:N `sys_fee_record`：一张账单可能对应多条支付记录（支付失败后重试）
- `sys_fee.house_id` → `sys_house.id`：每张账单归属一个房屋
- `sys_fee_record.user_id` → `sys_user.id`：记录是谁缴的

### 5.4 索引设计

| 表 | 索引 | 类型 | 用途 |
|----|------|------|------|
| `sys_fee` | PRIMARY KEY (`id`) | 聚簇索引 | 主键访问 |
| `sys_fee` | （隐式）`(house_id, fee_cycle, fee_type, status)` | 未单独建联合索引 | 查重校验：`FeeServiceImpl.java:306-318` |
| `sys_fee_record` | `idx_fee_id` | 普通索引 | 按账单查询缴费记录 |
| `sys_fee_record` | `idx_user_id` | 普通索引 | 我的缴费历史：`FeeServiceImpl.java:159-166` |
| `sys_fee_record` | `idx_order_no` | 普通索引 | 支付回调查单：`FeeServiceImpl.java:361-366` |

**设计原则**：
- 高频查询字段建索引（`fee_id`、`user_id` 出现在所有查询的 WHERE 条件里）
- 回调接口的关键字段建唯一性保障（`order_no` 虽然没建唯一索引，但业务代码做了状态检查防重复回调）

**缺失的优化**：`checkDuplicateBills()` 方法里 `WHERE house_id IN (...) AND fee_cycle = '...' AND fee_type = '...' AND status IN (...)`，这个查重场景应该加一个 `(house_id, fee_cycle, fee_type)` 联合索引，否则大量 house_id 去重时性能会下降。

### 5.5 分表策略

**当前阶段：不需要分表。**

- 一个社区 3000 户，每月产生 3000 条账单，一年 3.6 万条
- 缴费记录按每月 3000 条算，一年 3.6 万条
- MySQL 单表 500 万级别以内都无需分表

**如果未来需要分表，策略是**：

- **sys_fee**：按 `fee_cycle`（年月）分表 → `sys_fee_202501`, `sys_fee_202502`... 账单天然有周期属性，查询也按周期查
- **sys_fee_record**：按 `user_id` 取模分表 → `sys_fee_record_0`, `sys_fee_record_1`... 或按 `pay_time` 年份归档
- 用 ShardingSphere 配置分片规则，对应用层透明

### 5.6 接口设计

代码位置：`property-service/.../FeeServiceImpl.java`

| 接口 | 方法 | 说明 |
|------|------|------|
| 生成账单 | `generateBills()` | 管理员选择社区/楼栋/周期，面积 × 单价批量生成 |
| 查未缴账单 | `getCurrentUnpaid()` | 通过 Feign 调 HouseService 拿到用户绑定的房屋，再查未缴 |
| 缴费 | `payFee()` | 校验账单状态 → 生成支付记录（PENDING）→ 更新账单为 PAYING → 返回订单号 |
| 回调确认 | `payCallback()` | 验金额 → 更新记录为 SUCCESS → 更新账单为 PAID |
| 缴费历史 | `getPaymentHistory()` | 分页查询 SUCCESS 状态的缴费记录，关联账单表拿 feeCycle |
| 催缴 | `remind()` | 调 HouseService 拿业主列表 → 发系统通知 → 更新 remind_count |

---

## 第 6 题：Spring Cloud Alibaba 微服务架构

**题目**：你在项目中使用了 Spring Cloud Alibaba 微服务架构，请描述服务拆分的原则，以及如何通过 Nacos 配置管理和 Sentinel 限流降级来保证系统稳定性？具体遇到过哪些问题，如何解决的？

### 6.1 服务拆分原则

项目拆分为 8 个微服务 + 1 个 Gateway：

| 服务 | 职责 | 拆分理由 |
|------|------|---------|
| `gateway-service` | 统一入口、路由、限流 | 网关独立部署，不混入业务逻辑 |
| `user-service` | 用户注册/登录/管理 | 用户是独立领域，变更频率低 |
| `house-service` | 房屋/社区管理 | 房屋绑定审核流程独立 |
| `parking-service` | 车位/停车/计费 | 业务复杂度最高（出入库、计费、预留） |
| `property-service` | 物业费/投诉/公告/访客 | 物业日常运营核心 |
| `community-service` | 社区活动/话题/团购 | 社区互动独立，Redis+Lua 高并发场景 |
| `workorder-service` | 报修/工单/AI 分析 | 工单流转独立，含 AI 分析子模块 |
| `system-service` | 系统配置/操作日志/统计 | 横切面功能，被其他服务 Feign 调用 |
| `ai-service` | RAG 客服/AI 分析/运营洞察 | Java 17 独立服务，不接入 Nacos（Spring Boot 版本不同） |

**拆分原则**：
1. **按业务领域**：用户、房屋、停车、物业、社区活动，各自是高内聚的限界上下文
2. **按变更频率**：system-service（配置/日志）变更极少，community-service 活动报名经常迭代，拆分后互不影响
3. **按并发特征**：community-service 有高并发库存扣减（Redis+Lua），parking-service 有分布式锁场景，property-service 主要是 CRUD——不同并发特征拆开，可以用不同的资源配置
4. **数据库隔离**：每个服务只访问自己的表，服务间通过 Feign 或 MQ 通信，不直接访问对方数据库

### 6.2 Nacos 配置管理

**注册中心**：所有服务启动时向 Nacos 注册（`bootstrap.yml`），Gateway 通过 `lb://service-name` 做负载均衡路由。

**配置中心**：共享配置 `core-service.yaml` 存 Nacos，包含 MySQL/Redis/RabbitMQ/Sentinel/JWT 等全局配置。所有服务引同一份：
```yaml
spring:
  cloud:
    nacos:
      config:
        extension-configs:
          - data-id: core-service.yaml
            group: DEFAULT_GROUP
            refresh: true  # 配置变更自动刷新，不重启服务
```

好处：改 Redis 密码不需要重新部署 8 个服务，运维成本极大降低。

### 6.3 Sentinel 限流降级

**Gateway 层全局限流**（`gateway-service/application.yml`）：

```yaml
spring:
  cloud:
    sentinel:
      scg:
        fallback:
          mode: response
          response-status: 429
          response-body: '{"code":429,"msg":"请求过于频繁，请稍后再试","data":null}'
      datasource:
        gw-flow:          # 网关流控规则
          nacos:
            data-id: sentinel-gw-flow-rules
            rule-type: gw-flow
        gw-api-group:     # API 分组规则
          nacos:
            data-id: sentinel-gw-api-group-rules
            rule-type: gw-api-group
```

**system-service 层降级规则**（`system-service/application.yml`）：

```yaml
sentinel:
  datasource:
    param-flow:           # 热点参数限流
      nacos:
        data-id: sentinel-system-param-flow-rules
        rule-type: param-flow
    degrade:              # 熔断降级规则
      nacos:
        data-id: sentinel-system-degrade-rules
        rule-type: degrade
```

规则存在 Nacos 上，改了秒级生效，不需要重启。

### 6.4 遇到的问题和解决方案

**问题 1：分布式锁和事务的顺序问题**

停车场出闸接口——锁放在 `@Transactional` 方法里，锁释放了但事务还没提交。其他线程拿到锁读到旧数据，重复计费。

排查：两个线程同时调出闸，日志显示都拿到锁且都读到了出闸前的状态。定位到 AOP 代理和锁释放的顺序问题。

解决：改用 `TransactionTemplate`——先拿锁 → 开事务 → 提交事务 → 释放锁。保证锁内看到的一定是最新提交数据。

**问题 2：ai-service 与主服务版本不一致**

主服务是 Spring Boot 2.7.18 + Java 8，但 Spring AI 要求 Java 17 + Spring Boot 3.x。

解决：ai-service 独立部署在 8090 端口，不接入 Nacos。Gateway 用直连地址 `http://localhost:8090` 而非 `lb://ai-service`。Java 版本隔离，互不影响。

---

## 第 7 题：基于 Spring AI 接入 DeepSeek 实现 RAG 智能客服

**题目**：你提到基于 Spring AI 接入 DeepSeek 大模型实现 RAG 智能客服。请解释 RAG 的工作原理，以及你如何确保客服回答的准确性和抑制模型幻觉？

### 7.1 RAG 工作原理

RAG（Retrieval-Augmented Generation）的核心思想：**不让大模型凭记忆回答，先检索相关文档，再让模型基于文档回答**。

```
用户问题："我们小区下个月物业费什么时候交？"
    │
    ▼
① 检索 (Retrieve)：从社区知识库中找到相关文档
    │  关键字检索："物业费" 命中 notice #42
    │  向量检索：语义相似度 "缴费时间" 匹配到 notice #15
    │
    ▼
② 增强 (Augment)：把检索到的文档拼进 Prompt
    │  "请只根据以下社区资料回答：\n[资料1] 关于2025年物业费缴纳的通知...\n[资料2]..."
    │
    ▼
③ 生成 (Generate)：DeepSeek 基于文档生成回答
    │  "根据社区通知，2025年物业费需在1月31日前缴纳..."
    │
    ▼
④ 标准化 (Normalize)：校验 citations、置信度、cannotAnswer
    返回结构化对象: {answer, citations, confidence, cannotAnswer, followUpActions}
```

代码位置：`ai-service/.../RagCustomerServiceAssistant.java`

### 7.2 混合检索策略 — 提高召回准确率

代码位置：`ai-service/.../HybridCommunityKnowledgeRetriever.java`

**两路并行检索 + 合并排序：**

| 检索路 | 原理 | 优点 | 缺点 |
|--------|------|------|------|
| 关键字检索 | SQL LIKE + 关键词匹配 | 精确命中，不会跑偏 | 同义词/近义词匹配不到 |
| 向量检索 | Embedding → 余弦相似度 | 语义兜底，同义表达也能匹配 | 容易"碰瓷"（把语义相近但不相关的内容拉回来） |

**核心打分公式**（`MergeCandidate.mergedScore()`）：

```
vectorContribution = round(vectorScore × 0.35)
baseScore = max(keywordScore, vectorContribution)
if (关键字和向量都命中) baseScore += 5
```

为什么向量权重是 0.35？

- 关键字精确匹配的置信度远高于语义近似，关键字权重被设为基准
- 0.35 让向量检索在排序中能"露脸"（语义兜底），但不会盖过精确命中
- 双命中加 5 分，奖励"关键字和语义都匹配"的高质量结果

**降噪算法**（`suppressWeakVectorOnlyTail()`）：

```
情况 A：有关键字命中作为"锚点"
  → 纯向量结果必须 ≥ 锚点向量分 × 0.95，否则砍掉
情况 B：完全没关键字命中（纯靠向量）
  → 最高向量分 < 50 → 整批丢掉，告诉用户"没找到"
```

这样过滤掉"语义碰瓷"：比如用户问"物业费"，向量检索把"小区绿化"也拉回来（语义相似但完全不相关），降噪算法直接砍掉。

### 7.3 抑制模型幻觉 — 四重保障

**第一重：System Prompt 硬约束**

```java
private static final String SYSTEM_PROMPT = String.join("\n",
    "你是智能社区客服助手。",
    "你只能根据提供的社区资料回答问题，不能编造公告、制度、费用、时间或工作人员信息。",
    "如果资料不足以回答问题，cannotAnswer 必须为 true，并提示居民联系物业客服人工确认。"
);
```

明确告诉模型：**只能基于资料回答，资料不够就说不知道**。

**第二重：检索结果作为唯一知识来源**

Prompt 中明确区分"居民问题"和"社区资料"，模型的回答必须来源于社区资料，而不是预训练记忆。

**第三重：输出标准化校验 (Normalizer)**

`CustomerServiceAnswerNormalizer` 对模型输出做：
- **citations 白名单校验**：输出的引用必须来自检索结果的 `sourceId`，不允许模型编造假引用
- **confidence 上限**：检索到的资料数量不够时强制降低 confidence
- **cannotAnswer 兜底**：资料为空时直接返回"请联系物业客服"，不调 LLM（省钱 + 安全）

**第四重：异常降级**

```java
} catch (RuntimeException ex) {
    // LLM 不可用时，返回检索结果本身作为回答
    return normalizer.normalize(retrievalFallback(documents), documents, ...);
}
```

大模型挂了也不返回空，而是把检索到的资料原文返回给用户。

**第五重：可观测性**

每次 AI 调用记录到 `ai_call_log` 表：
- `biz_type`：业务类型（CUSTOMER_SERVICE_RAG / WORK_ORDER_ANALYSIS）
- `provider` / `model` / `provider_version`：追踪不同模型版本效果
- `status`：SUCCESS / FALLBACK / FAILED
- `confidence`：模型自信度
- `latency_ms`：每次调用的耗时

有了这些日志，哪些问题模型回答得不好、哪些场景走了降级，全都可以追溯和改进。

---

## 第 8 题：Java 内存模型 (JMM) 与 happens-before 原则

**题目**：请解释 Java 内存模型（JMM）中的 happens-before 原则，并结合你的项目说明如何利用它保证并发程序的正确性？

### 8.1 JMM 要解决的问题

Java 内存模型定义了一套规则，规定**一个线程的写操作何时对另一个线程可见**。没有 JMM 的话：
- CPU 缓存导致线程 A 写的值线程 B 看不到
- 编译器/CPU 指令重排序导致代码执行顺序和写的顺序不一致

JMM 解决这两个问题的核心就是 **happens-before 原则**。

### 8.2 8 条 happens-before 规则

| 规则 | 含义 | 典型场景 |
|------|------|---------|
| **程序次序** | 单线程内，前面的操作 happens-before 后面的 | `x=1; y=2;` x 的写入对 y 的读取可见 |
| **锁规则 (Monitor)** | 解锁 happens-before 后续加锁 | `synchronized`、`ReentrantLock`、分布式锁 |
| **volatile 规则** | volatile 变量的写 happens-before 后续读 | 状态标志位、配置开关 |
| **线程启动** | `thread.start()` happens-before 线程内任意操作 | 传参给新线程保证可见 |
| **线程终止** | 线程内任意操作 happens-before `thread.join()` 返回 | 等子线程结束拿结果 |
| **中断规则** | `interrupt()` happens-before 被中断线程检测到中断 | 优雅停机 |
| **传递性** | A hb B 且 B hb C → A hb C | 间接推导 |
| **final 规则** | 构造器中 final 字段赋值 happens-before 其他线程读到该对象 | 不可变对象安全发布 |

### 8.3 项目中的实际应用

#### 场景 1：锁规则 — @Transactional + Redis 分布式锁的顺序保证

**问题**：原来锁放在 @Transactional 方法里 → AOP 代理 → 锁释放了但事务还没提交 → 其他线程拿到锁读到旧数据。

**解决**：TransactionTemplate 控制顺序 — 锁释放 happens-before 下一个线程拿到锁，满足锁规则。

```
线程 A：拿锁 → 开事务 → 提交事务 → 释放锁
                                            │
                          happens-before (锁规则)
                                            │
                                            ▼
线程 B：                                    拿锁 → 读到的数据一定包含 A 已提交的修改
```

本质上就是利用 `ReentrantLock.unlock()` happens-before `ReentrantLock.lock()` 的 JMM 保证。

#### 场景 2：volatile 规则 — Redis Lua 脚本的状态保证

Lua 脚本在 Redis 中单线程执行，等价于 volatile 语义——脚本中对 `stockKey` 的 `DECR` 操作 happens-before 下一个脚本对同一 key 的读。

```lua
-- 脚本执行 #1（线程 A）
local stock = redis.call('DECR', stockKey)  -- 写操作

-- happens-before（Redis 单线程保证）

-- 脚本执行 #2（线程 B）
if redis.call('EXISTS', stockKey) == 0      -- 读操作，一定看到 #1 的 DECR 结果
```

Redis 单线程模型天然提供了 happens-before 保证。

#### 场景 3：传递性 — MQ 消息投递

```
Redis Lua 扣库存 → (happens-before) → MQ 消息发送 → (happens-before) → MQ 消费者落库
```

RabbitMQ 的 publisher confirm + 手动 ack，保证了消息的可靠传递。结合传递性：Lua 扣库存的结果一定对 MQ 消费者可见。

#### 场景 4：final 规则 — Spring Bean 不可变配置

```java
@Value("${spring.ai.openai.chat.options.model}")
private String model;  // Spring 保证在 Bean 初始化完成前赋值
```

Spring 容器保证了 Bean 初始化完成后，所有依赖注入的字段对其他 Bean 可见。这等价于 final 规则——构造完成后，所有字段的赋值对后续读取可见。

### 8.4 总结

happens-before 不是什么"高级面试知识点"，而是**日常写并发代码时防止莫名其妙 bug 的理论基础**。我用它的方式不是背 8 条规则，而是判断：**"这段代码执行后，结果对另一个线程可见吗？通过什么机制保证？"**
