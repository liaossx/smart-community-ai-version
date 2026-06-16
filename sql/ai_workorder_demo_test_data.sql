-- ============================================================
-- AI work order assistant demo data
-- Purpose:
--   Prepare layered repair reports for the AI work order assistant.
--   These cases are designed to verify:
--   1. category classification
--   2. priority differentiation
--   3. high-risk keyword escalation
--   4. recommended team and suggested action quality
--   5. manualReviewNeeded on ambiguous or dangerous cases
--
-- How to use:
--   1. Execute sql/workorder_ai_analysis_schema.sql once if not already created
--   2. Execute this script in smart_community
--   3. Call:
--      POST /api/workorder/ai/repair/{repairId}/analyze
--      GET  /api/workorder/ai/repair/{repairId}/latest
--      GET  /api/workorder/ai/repair/{repairId}/history
--
-- Suggested test ids:
--   9101 low water
--   9102 medium public facility
--   9103 medium elevator
--   9104 critical elevator trapped person
--   9105 critical electrical leakage
--   9106 critical gas smell
--   9107 critical flooding / access blocked
--   9108 ambiguous damp floor
-- ============================================================

SET NAMES utf8mb4;
SET @ops_community_id := 1;

INSERT INTO sys_community (id, name, address, phone, description, create_time, update_time)
SELECT @ops_community_id, 'AI工单演示社区', 'AI工单测试地址', '0755-26059999', 'AI工单助手演示社区', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_community WHERE id = @ops_community_id
);

SET @ops_community_name := (
    SELECT name FROM sys_community WHERE id = @ops_community_id LIMIT 1
);

INSERT INTO sys_user (username, password, real_name, phone, role, status, community_id, create_time, update_time)
VALUES ('ai_workorder_owner_001', '123456', 'AI工单测试业主', '19926053001', 'OWNER', 1, @ops_community_id, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    real_name = VALUES(real_name),
    role = VALUES(role),
    status = VALUES(status),
    community_id = VALUES(community_id),
    update_time = NOW();
SET @ops_user_id := LAST_INSERT_ID();

INSERT INTO sys_house
(community_id, community_name, building_no, house_no, area, is_default, bind_status, floor, type, create_time, update_time)
VALUES
(@ops_community_id, @ops_community_name, 'AI工单1栋', '101', 92.00, 1, 1, 1, '两室一厅', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    community_id = VALUES(community_id),
    area = VALUES(area),
    bind_status = VALUES(bind_status),
    update_time = NOW();
SET @ops_house_id := LAST_INSERT_ID();

DELETE FROM biz_work_order_ai_analysis
WHERE repair_id BETWEEN 9101 AND 9108;

DELETE FROM biz_work_order
WHERE repair_id BETWEEN 9101 AND 9108
   OR order_no LIKE 'AI-WO-DEMO-%';

DELETE FROM biz_repair
WHERE id BETWEEN 9101 AND 9108
   OR fault_imgs = 'AI_WORKORDER_DEMO';

INSERT INTO biz_repair
(id, user_id, house_id, fault_type, fault_desc, fault_imgs, status, handle_remark, create_time, update_time, community_id)
VALUES
(9101, @ops_user_id, @ops_house_id, '水管', '厨房水龙头滴水，已经持续两天，平时还能正常使用。', 'AI_WORKORDER_DEMO', 'pending', 'AI工单演示：普通低风险', '2026-06-10 09:00:00', '2026-06-10 09:00:00', @ops_community_id),
(9102, @ops_user_id, @ops_house_id, '公共设施', '单元门门禁识别很慢，早高峰排队明显，但还能正常开门。', 'AI_WORKORDER_DEMO', 'pending', 'AI工单演示：中风险公共设施', '2026-06-10 09:10:00', '2026-06-10 09:10:00', @ops_community_id),
(9103, @ops_user_id, @ops_house_id, '电梯', '电梯运行时有异响，暂时没有困人，但住户担心继续运行不安全。', 'AI_WORKORDER_DEMO', 'pending', 'AI工单演示：中风险电梯', '2026-06-10 09:20:00', '2026-06-10 09:20:00', @ops_community_id),
(9104, @ops_user_id, @ops_house_id, '电梯', '3栋电梯有人被困，按了紧急按钮后仍未脱困。', 'AI_WORKORDER_DEMO', 'pending', 'AI工单演示：高危困人', '2026-06-10 09:30:00', '2026-06-10 09:30:00', @ops_community_id),
(9105, @ops_user_id, @ops_house_id, '电路', '厨房插座疑似漏电，住户反映闻到焦糊味。', 'AI_WORKORDER_DEMO', 'pending', 'AI工单演示：高危漏电', '2026-06-10 09:40:00', '2026-06-10 09:40:00', @ops_community_id),
(9106, @ops_user_id, @ops_house_id, '燃气', '楼道里有明显燃气味，晚上味道更重，住户担心有泄漏。', 'AI_WORKORDER_DEMO', 'pending', 'AI工单演示：高危燃气', '2026-06-10 09:50:00', '2026-06-10 09:50:00', @ops_community_id),
(9107, @ops_user_id, @ops_house_id, '环境', '暴雨后地下车库大面积积水，车辆进出困难，部分区域无法通行。', 'AI_WORKORDER_DEMO', 'pending', 'AI工单演示：高危积水阻断', '2026-06-10 10:00:00', '2026-06-10 10:00:00', @ops_community_id),
(9108, @ops_user_id, @ops_house_id, '其他', '卫生间门口地面有点潮，不确定是不是漏水，麻烦帮忙看一下。', 'AI_WORKORDER_DEMO', 'pending', 'AI工单演示：模糊待确认', '2026-06-10 10:10:00', '2026-06-10 10:10:00', @ops_community_id);

SELECT
    'AI work order demo data inserted' AS message,
    @ops_community_id AS communityId,
    @ops_community_name AS communityName,
    @ops_user_id AS userId,
    @ops_house_id AS houseId,
    8 AS repairCount;
