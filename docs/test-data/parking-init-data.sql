-- 停车管理系统初始化测试数据
-- 执行前请确保已创建表结构（执行 docs/sql/parking.sql）

-- ============================================
-- 1. 插入临时停车位（TEMP）
-- ============================================
INSERT INTO biz_parking_space (community_id, community_name, space_no, space_type, status, deleted, create_time, update_time) VALUES
(1, '阳光花园', 'TEMP-001', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'TEMP-002', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'TEMP-003', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'TEMP-004', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'TEMP-005', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(NULL, NULL, 'TEMP-006', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(NULL, NULL, 'TEMP-007', 'TEMP', 'AVAILABLE', 0, NOW(), NOW()),
(NULL, NULL, 'TEMP-008', 'TEMP', 'AVAILABLE', 0, NOW(), NOW());

-- ============================================
-- 2. 插入固定停车位（FIXED）
-- ============================================
INSERT INTO biz_parking_space (community_id, community_name, space_no, space_type, status, deleted, create_time, update_time) VALUES
(1, '阳光花园', 'FIXED-A01', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-A02', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-A03', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-B01', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-B02', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-C01', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-C02', 'FIXED', 'AVAILABLE', 0, NOW(), NOW()),
(1, '阳光花园', 'FIXED-D01', 'FIXED', 'AVAILABLE', 0, NOW(), NOW());

-- ============================================
-- 说明：
-- ============================================
-- 1. 临时车位：TEMP-001 到 TEMP-008（共8个）
-- 2. 固定车位：FIXED-A01 到 FIXED-D01（共8个）
-- 3. 车位ID会自动生成（从1开始自增）
-- 4. 如果需要指定小区，请修改 community_id 和 community_name
-- 5. 如果不需要指定小区，community_id 和 community_name 可以为 NULL

-- ============================================
-- 查询插入的数据
-- ============================================
-- SELECT * FROM biz_parking_space ORDER BY id;

-- ============================================
-- 如果需要清空数据重新开始
-- ============================================
-- DELETE FROM biz_parking_space;
-- ALTER TABLE biz_parking_space AUTO_INCREMENT = 1;



