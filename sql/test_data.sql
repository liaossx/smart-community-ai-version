-- 1. 确保已有社区数据
INSERT INTO sys_community (id, name, address, linkman, phone) VALUES 
(1, '幸福小区', '幸福路1号', '张三', '13800000001'),
(2, '阳光小区', '阳光路88号', '李四', '13800000002')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. 确保管理员和普通用户存在，并绑定社区
-- admin (超级管理员)
-- admin1 (幸福小区管理员)
-- owner1 (幸福小区业主)
-- owner2 (阳光小区业主)

-- 注意：密码通常是加密的，这里假设已有用户，仅更新 community_id
-- 假设 id=1 是超级管理员，不绑定社区
UPDATE sys_user SET community_id = NULL WHERE username = 'admin';

-- 假设存在普通管理员 admin1，绑定到 1号社区
UPDATE sys_user SET community_id = 1, role = 'admin' WHERE username = 'lsx';

-- 假设存在业主 owner1，绑定到 1号社区
UPDATE sys_user SET community_id = 1, role = 'owner' WHERE username = 'owner1';

-- 假设存在业主 owner2，绑定到 2号社区
UPDATE sys_user SET community_id = 2, role = 'owner' WHERE username = 'owner2';

-- 2.1 增加维修员数据
-- 密码均为 123456 (加密后)
INSERT INTO sys_user (username, password, real_name, phone, role, community_id, status, create_time, update_time) VALUES
('worker1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张师傅', '13511112222', 'worker', 1, 1, NOW(), NOW()),
('worker2', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李师傅', '13533334444', 'worker', 1, 1, NOW(), NOW());


-- 3. 访客数据 (sys_visitor)
INSERT INTO sys_visitor (user_id, community_id, visitor_name, visitor_phone, reason, visit_time, car_no, status, create_time) VALUES
(1, 1, '王五', '13900000001', '探亲', DATE_ADD(NOW(), INTERVAL 1 DAY), '粤A12345', 'PENDING', NOW()),
(1, 1, '赵六', '13900000002', '送货', DATE_ADD(NOW(), INTERVAL 2 DAY), '粤B23456', 'APPROVED', NOW()),
(2, 2, '孙七', '13900000003', '维修', DATE_ADD(NOW(), INTERVAL 3 DAY), NULL, 'REJECTED', NOW());

-- 4. 投诉建议数据 (sys_complaint)
INSERT INTO sys_complaint (user_id, community_id, type, content, status, create_time) VALUES
(1, 1, '噪音扰民', '楼上装修太吵了', 'PENDING', NOW()),
(1, 1, '环境卫生', '垃圾桶满了没人倒', 'PROCESSED', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 2, '设施故障', '路灯坏了', 'PENDING', NOW());

-- 5. 社区活动数据 (sys_activity)
INSERT INTO sys_activity (community_id, title, content, start_time, location, max_count, signup_count, status, create_time) VALUES
(1, '社区篮球赛', '欢迎大家报名参加篮球赛', DATE_ADD(NOW(), INTERVAL 7 DAY), '篮球场', 20, 5, 'PUBLISHED', NOW()),
(1, '端午节包粽子', '端午节活动', DATE_ADD(NOW(), INTERVAL 10 DAY), '活动中心', 50, 0, 'DRAFT', NOW()),
(2, '阳光小区义诊', '免费测量血压血糖', DATE_ADD(NOW(), INTERVAL 5 DAY), '小区广场', 100, 10, 'PUBLISHED', NOW());

-- 6. 物业费数据 (sys_fee)
-- 假设 house_id 1 属于社区 1
INSERT INTO sys_fee (house_id, community_id, building_no, fee_cycle, fee_amount, fee_type, status, due_date, create_time) VALUES
(1, 1, '1栋', '2023-10', 200.00, '物业费', 'UNPAID', '2023-10-31 23:59:59', NOW()),
(1, 1, '1栋', '2023-09', 200.00, '物业费', 'PAID', '2023-09-30 23:59:59', DATE_SUB(NOW(), INTERVAL 1 MONTH));

-- 7. 停车订单数据 (biz_parking_order) - 补充 community_id
-- 确保之前的订单也有 community_id (示例)
UPDATE biz_parking_order SET community_id = 1 WHERE id IN (1, 2, 3);
UPDATE biz_parking_order SET community_id = 2 WHERE id IN (4, 5);

-- 8. 补充车位数据 (biz_parking_space)
INSERT INTO biz_parking_space (community_id, community_name, space_no, space_type, status, deleted, create_time) VALUES
(1, '幸福小区', 'A-001', 'FIXED', 'AVAILABLE', 0, NOW()),
(1, '幸福小区', 'A-002', 'TEMP', 'AVAILABLE', 0, NOW()),
(2, '阳光小区', 'B-001', 'FIXED', 'AVAILABLE', 0, NOW());

-- 9. 补充房屋数据 (sys_house) (如果需要关联)
-- INSERT INTO sys_house (community_name, building_no, house_no, area) VALUES ('幸福小区', '1栋', '101', 90.00);

-- 10. 补充公告数据 (sys_notice)
INSERT INTO sys_notice (title, content, target_type, community_id, community_name, publish_status, create_time) VALUES
('幸福小区停水通知', '因管道维修，明天停水', 'COMMUNITY', 1, '幸福小区', 'PUBLISHED', NOW()),
('阳光小区消杀通知', '明天进行全区消杀', 'COMMUNITY', 2, '阳光小区', 'PUBLISHED', NOW());

