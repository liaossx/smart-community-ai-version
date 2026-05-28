-- =======================================================
-- 测试数据生成脚本 (基于用户真实环境)
-- =======================================================

-- 1. 访客预约数据 (sys_visitor)
-- 场景：不同社区的业主预约访客，验证管理员只能看到本社区的记录
-- 社区1(幸福家园)管理员(lsx, id=7) 应该能看到 id=1,2 的记录
-- 社区4(阳光小区)管理员(admin, id=1) 应该能看到 id=3 的记录
INSERT INTO sys_visitor (user_id, community_id, visitor_name, visitor_phone, reason, visit_time, car_no, status, create_time) VALUES
(7, 1, '张三丰', '13900001111', '探亲', DATE_ADD(NOW(), INTERVAL 1 DAY), '粤A88888', 'PENDING', NOW()),
(7, 1, '李寻欢', '13900002222', '送货', DATE_ADD(NOW(), INTERVAL 2 DAY), '粤B66666', 'APPROVED', NOW()),
(2, 4, '楚留香', '13900003333', '聚会', DATE_ADD(NOW(), INTERVAL 3 DAY), NULL, 'PENDING', NOW());

-- 2. 投诉建议数据 (sys_complaint)
-- 场景：验证不同社区管理员只能处理本社区投诉
-- 社区1(幸福家园)管理员(lsx) 可见 id=1
-- 社区4(阳光小区)业主(owner_1) 提交，社区4管理员(admin) 可见 id=2
INSERT INTO sys_complaint (user_id, community_id, type, content, status, create_time) VALUES
(7, 1, '噪音扰民', '楼上装修太吵了，请物业协调', 'PENDING', NOW()),
(2, 4, '环境卫生', '小区门口垃圾桶满了', 'PROCESSED', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 4, '设施故障', '路灯坏了', 'PENDING', NOW());

-- 3. 社区活动数据 (sys_activity)
-- 场景：发布不同社区的活动
-- 社区1(幸福家园)活动
INSERT INTO sys_activity (community_id, title, content, start_time, location, max_count, signup_count, status, create_time) VALUES
(1, '幸福家园篮球友谊赛', '欢迎各位业主踊跃报名', DATE_ADD(NOW(), INTERVAL 7 DAY), '篮球场', 20, 5, 'PUBLISHED', NOW()),
(1, '社区义诊活动', '免费量血压、测血糖', DATE_ADD(NOW(), INTERVAL 10 DAY), '活动中心', 100, 0, 'DRAFT', NOW());

-- 社区4(阳光小区)活动
INSERT INTO sys_activity (community_id, title, content, start_time, location, max_count, signup_count, status, create_time) VALUES
(4, '阳光小区亲子运动会', '增进亲子感情', DATE_ADD(NOW(), INTERVAL 5 DAY), '中心花园', 50, 10, 'PUBLISHED', NOW());

-- 4. 物业费数据 (sys_fee)
-- 场景：为真实存在的房屋生成账单
-- 关联房屋：id=1 (阳光小区, 1栋101), community_id=4 (虽然你给的 sys_house 社区是阳光小区但 community_id=1? 
-- 这里根据 sys_community 表，阳光小区 id=4。
-- 注意：你提供的 sys_house 数据中 community_id=1，但 community_name='阳光小区'，而 sys_community 中 '阳光小区' id=4。
-- 这可能是历史数据不一致。为了测试，我假设房屋 id=1 属于社区 4 (阳光小区)。
-- 同时也为社区 1 (幸福家园) 插入一条虚拟房屋账单用于测试 lsx (id=7) 的权限。

-- 修正：先插入一条属于 幸福家园(id=1) 的房屋数据，方便 lsx 管理员测试
INSERT INTO sys_house (id, community_name, building_no, house_no, area, community_id) 
VALUES (999, '幸福家园', '8栋', '808', 100.00, 1) 
ON DUPLICATE KEY UPDATE community_id=1;

-- 插入账单
-- 属于 幸福家园(id=1) 的账单 -> lsx 可见
INSERT INTO sys_fee (house_id, community_id, building_no, fee_cycle, fee_amount, fee_type, status, due_date, create_time) VALUES
(999, 1, '8栋', '2025-02', 200.00, '物业费', 'UNPAID', '2025-02-28 23:59:59', NOW());

-- 属于 阳光小区(id=4) 的账单 -> admin 可见
INSERT INTO sys_fee (house_id, community_id, building_no, fee_cycle, fee_amount, fee_type, status, due_date, create_time) VALUES
(1, 4, '1栋', '2025-02', 150.00, '物业费', 'UNPAID', '2025-02-28 23:59:59', NOW()),
(1, 4, '1栋', '2025-01', 150.00, '物业费', 'PAID', '2025-01-31 23:59:59', DATE_SUB(NOW(), INTERVAL 1 MONTH));

-- 5. 停车订单数据 (biz_parking_order)
-- 场景：不同社区的停车订单
INSERT INTO biz_parking_order (community_id, order_no, user_id, space_id, order_type, amount, status, start_time, create_time, update_time) VALUES
(1, 'PK202602260001', 7, NULL, 'TEMP', 10.00, 'UNPAID', NOW(), NOW(), NOW()), -- lsx 在幸福家园停车
(4, 'PK202602260002', 2, NULL, 'TEMP', 5.00, 'PAID', NOW(), NOW(), NOW());   -- owner_1 在阳光小区停车

-- 6. 车位数据 (biz_parking_space)
INSERT INTO biz_parking_space (community_id, community_name, space_no, space_type, status, deleted, create_time) VALUES
(1, '幸福家园', 'A-001', 'FIXED', 'AVAILABLE', 0, NOW()),
(4, '阳光小区', 'B-888', 'TEMP', 'AVAILABLE', 0, NOW());

