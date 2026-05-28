# 智慧社区微服务系统 (Smart Community)

## 快速启动指南

### 1. 基础设施启动
本项目依赖 MySQL, Redis, Nacos, RabbitMQ, Sentinel。
已提供 `docker-compose.yml` 用于一键启动环境。

```bash
docker-compose up -d
```

### 2. Nacos 配置导入
项目各服务依赖 Nacos 配置中心。请在 Nacos 启动后 (http://localhost:8848/nacos, 默认账号 nacos/nacos) 执行以下操作：
1.  **创建命名空间**：保留默认 `public` 即可（bootstrap.yml 未指定 namespace）。
2.  **创建配置**：在 `DEFAULT_GROUP` 下创建 DataId: `core-service.yaml`。
3.  **配置内容**：将 `nacos_config/core-service.yaml` 的内容复制进去。
    *   *注意*：该文件已修正了包名和 Mapper 路径，请务必使用该版本，不要使用旧版本。
4.  **停车服务配置**：同样创建 `parking-service.yaml`（如果需要独立配置）。
5.  **内部调用 Token**：`core-service.yaml` 内含 `internal.token`，用于内部接口（如通知、模拟支付回调）鉴权；调用方需带 `X-Internal-Token` 请求头。
6.  **模拟支付开关**：`mock.payment.enabled` 默认关闭；需要使用模拟回调时在 Nacos 或环境变量 `MOCK_PAYMENT_ENABLED=true` 打开。
7.  **真实支付回调验签**：回调接口要求 `X-Pay-Timestamp`、`X-Pay-Nonce`、`X-Pay-Sign` 请求头，并使用 `payment.secret` 做 HMAC-SHA256 验签；时间窗由 `payment.allowedSkewSeconds` 控制。

### 3. 数据库初始化
`docker-compose` 启动时会自动加载 `sql/init_all.sql` 初始化 `smart_community` 数据库。
如果手动启动 MySQL，请手动执行 `sql/init_all.sql`。

### 4. 服务启动顺序
1.  GatewayService
2.  SystemService (提供基础支撑)
3.  UserService
4.  HouseService
5.  PropertyService / WorkorderService / ParkingService / CommunityService

## 端口说明
- Nacos: 8848
- Sentinel: 8858
- RabbitMQ: 5672 (管理面板 15672)
- MySQL: 3306
- Redis: 6379
- Gateway: 80 (注意 Windows 下可能需要管理员权限或修改端口)

## 常见问题
- **包名错误**：原 `core-service.yaml` 中包含 `com.lsx.core` 包名，与实际业务服务 `com.lsx.{service}` 不符，已在 `nacos_config/core-service.yaml` 中修正。
- **RabbitMQ 消息**：报修通知会通过 MQ 异步触发站内信；消费者侧做了幂等处理，重复投递不会重复创建通知。
- **支付回调被拒绝**：检查 Nacos 中的 `payment.secret` 与回调端签名是否一致，并确保时间戳在允许窗口内（默认 300 秒）。
