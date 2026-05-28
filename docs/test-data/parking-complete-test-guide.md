# 停车管理系统完整测试指南

## 测试顺序说明

⚠️ **重要提示**：需要先准备基础数据（用户、车位），然后按照以下顺序测试：

---

## 第一步：准备基础数据

### 1.1 确保数据库中有用户数据

如果没有用户，需要先创建：
- 至少需要1个业主（role = "owner"）
- 建议用户ID：1, 2, 3

### 1.2 在数据库中初始化车位数据

执行以下SQL语句创建测试车位：

```sql
-- 插入临时停车位（TEMP）
INSERT INTO biz_parking_space (community_id, community_name, space_no, space_type, status, deleted, create_time, update_time) VALUES
(1, '阳光花园', 'TEMP-001', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'TEMP-002', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'TEMP-003', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'TEMP-004', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'TEMP-005', 'TEMP', 'AVAILABLE', 0, NOW(), NOW());

-- 插入固定停车位（FIXED）
INSERT INTO biz_parking_space (community_id, community_name, space_no, space_type, status, deleted, create_time, update_time) VALUES
(1, '阳光花园', 'FIXED-A01', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-A02', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-A03', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-B01', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-B02', 'FIXED', 'AVAILABLE', 0, NOW(), NOW());
```

**说明**：
- 临时车位：TEMP-001 到 TEMP-005（共5个）
- 固定车位：FIXED-A01, A02, A03, B01, B02（共5个）
- 车位ID会自动生成（1-10）

---

## 第二步：测试车位管理接口

### 2.1 查询可用车位数量

**接口**：`GET /api/parking/space/remaining`

**测试1：查询所有小区的车位数量**
```
GET http://localhost:8081/api/parking/space/remaining
```

**测试2：查询指定小区的车位数量**
```
GET http://localhost:8081/api/parking/space/remaining?communityId=1
```

**预期结果**：
- 临时车位剩余：5
- 固定车位剩余：5

---

### 2.2 绑定固定车位（必须先有房屋ID）

**接口**：`POST /api/parking/space/bind`

**测试数据1：绑定第一个固定车位**
```json
{
  "spaceId": 6,
  "userId": 1,
  "houseId": 1,
  "leaseEndTime": "2025-12-31T23:59:59"
}
```

**测试数据2：绑定第二个固定车位**
```json
{
  "spaceId": 7,
  "userId": 2,
  "houseId": 2,
  "leaseEndTime": "2025-12-31T23:59:59"
}
```

**注意事项**：
- `spaceId` 必须是固定车位（FIXED），且状态为 AVAILABLE
- `userId` 必须是已存在的用户ID
- `houseId` 必须是已存在的房屋ID

---

### 2.3 查询我绑定的车位

**接口**：`GET /api/parking/space/my?userId=1`

**测试**：
```
GET http://localhost:8081/api/parking/space/my?userId=1
```

**预期结果**：返回用户1绑定的所有车位列表

---

### 2.4 授权车位给访客使用

**接口**：`POST /api/parking/space/{spaceId}/authorize`

**前置条件**：必须先绑定车位（spaceId 6 已绑定给 userId 1）

**测试数据1：授权给访客A**
```json
{
  "userId": 1,
  "authorizedName": "张三",
  "authorizedPhone": "13800138001",
  "plateNo": "京A12345",
  "endTime": "2025-01-20T23:59:59"
}
```

**请求URL**：`POST http://localhost:8081/api/parking/space/6/authorize`

**测试数据2：授权给访客B**
```json
{
  "userId": 1,
  "authorizedName": "李四",
  "authorizedPhone": "13800138002",
  "plateNo": "京B88888",
  "endTime": "2025-01-25T23:59:59"
}
```

**请求URL**：`POST http://localhost:8081/api/parking/space/6/authorize`

---

### 2.5 查询我的授权记录

**接口**：`GET /api/parking/authorize/my`

**测试**：
```
GET http://localhost:8081/api/parking/authorize/my?userId=1&pageNum=1&pageSize=10
```

**预期结果**：返回用户1的所有授权记录（分页）

---

## 第三步：测试停车订单接口

### 3.1 创建临时停车订单

**接口**：`POST /api/parking/order`

**测试数据1：临时停车2小时**
```json
{
  "userId": 1,
  "orderType": "TEMP",
  "amount": 10.00,
  "startTime": "2025-01-15T09:00:00",
  "endTime": "2025-01-15T11:00:00",
  "remark": "京A12345"
}
```

**测试数据2：临时停车全天**
```json
{
  "userId": 2,
  "orderType": "TEMP",
  "amount": 50.00,
  "startTime": "2025-01-15T08:00:00",
  "endTime": "2025-01-15T20:00:00",
  "remark": "临时停车"
}
```

**注意**：临时订单不需要 `spaceId`

---

### 3.2 创建固定车位订单

**前置条件**：必须已经绑定了固定车位

**测试数据：固定车位月租订单**
```json
{
  "userId": 1,
  "spaceId": 6,
  "orderType": "FIXED",
  "amount": 300.00,
  "startTime": "2025-02-01T00:00:00",
  "endTime": "2025-02-28T23:59:59",
  "remark": "2月固定车位月租"
}
```

**注意**：固定车位订单必须提供 `spaceId`

---

### 3.3 支付停车订单

**接口**：`PUT /api/parking/order/{orderId}/pay`

**前置条件**：先创建一个订单，获取订单ID（假设返回的订单ID是 1）

**测试数据1：微信支付**
```json
{
  "userId": 1,
  "payChannel": "WECHAT",
  "payRemark": "微信支付-交易号：wx2025011514300001"
}
```

**请求URL**：`PUT http://localhost:8081/api/parking/order/1/pay`

**测试数据2：支付宝支付**
```json
{
  "userId": 1,
  "payChannel": "ALIPAY",
  "payRemark": "支付宝支付-交易号：2025011514300001"
}
```

**测试数据3：现金支付**
```json
{
  "userId": 1,
  "payChannel": "CASH",
  "payRemark": "现金支付-已收讫"
}
```

---

### 3.4 查询我的订单列表

**接口**：`GET /api/parking/order/my`

**测试**：
```
GET http://localhost:8081/api/parking/order/my?userId=1&pageNum=1&pageSize=10
```

**预期结果**：返回用户1的所有订单（分页，按创建时间倒序）

---

## 完整测试流程示例

### 场景1：临时停车流程

1. ✅ 查询可用车位数量 → 确认有临时车位
2. ✅ 创建临时停车订单 → 获取订单ID
3. ✅ 支付订单 → 使用微信/支付宝/现金支付
4. ✅ 查询订单列表 → 确认订单状态为已支付

### 场景2：固定车位流程

1. ✅ 查询可用车位数量 → 确认有固定车位
2. ✅ 绑定固定车位 → 绑定车位ID 6 给用户1
3. ✅ 查询我的车位 → 确认绑定成功
4. ✅ 创建固定车位订单 → 获取订单ID
5. ✅ 支付订单 → 支付月租费用
6. ✅ 查询订单列表 → 确认订单状态

### 场景3：车位授权流程

1. ✅ 绑定固定车位 → 绑定车位ID 6 给用户1
2. ✅ 授权车位给访客 → 创建授权记录
3. ✅ 查询授权记录 → 确认授权成功
4. ✅ 查询我的车位 → 查看绑定和授权情况

---

## 测试数据总结

### 车位数据（需要在数据库中先创建）
- **临时车位ID**：1-5（TEMP-001 到 TEMP-005）
- **固定车位ID**：6-10（FIXED-A01, A02, A03, B01, B02）

### 用户数据（需要已存在）
- **用户ID**：1, 2, 3（建议使用已存在的用户）
- **用户角色**：owner（业主）

### 房屋数据（需要已存在）
- **房屋ID**：1, 2（用于绑定车位）

---

## 常见错误提示

1. **车位不存在**：检查 spaceId 是否正确，是否已在数据库中创建
2. **无权绑定该车位**：确保车位类型是 FIXED，状态是 AVAILABLE
3. **无权授权该车位**：确保车位已绑定给当前用户
4. **订单不存在或已支付**：检查订单ID和订单状态
5. **固定车位订单必须指定车位ID**：orderType=FIXED 时必须提供 spaceId

---

## 快速测试SQL（可选）

如果需要快速重置测试数据，可以使用以下SQL：

```sql
-- 重置车位状态为可用
UPDATE biz_parking_space SET status = 'AVAILABLE', bind_user_id = NULL, bind_house_id = NULL, bind_time = NULL, lease_end_time = NULL WHERE id <= 10;

-- 清空订单数据（谨慎使用）
-- DELETE FROM biz_parking_order;

-- 清空授权数据（谨慎使用）
-- DELETE FROM biz_parking_authorize;
```



