-- ============================================================
-- AI 后台运营周报测试数据
-- 目的：给 /api/ai/operations/weekly-report/from-db 准备不同数据量场景
--
-- 使用方式：
-- 1. 在 MySQL smart_community 库执行本脚本。
-- 2. 重启或保持 ai-service 运行。
-- 3. 分别请求下面 3 个日期区间观察 AI 周报变化：
--    低量场景：
--    GET /api/ai/operations/weekly-report/from-db?communityId=1&startDate=2026-05-18&endDate=2026-05-24
--    常规场景：
--    GET /api/ai/operations/weekly-report/from-db?communityId=1&startDate=2026-05-25&endDate=2026-05-31
--    高风险高量场景：
--    GET /api/ai/operations/weekly-report/from-db?communityId=1&startDate=2026-06-01&endDate=2026-06-07
--
-- 本脚本可重复执行：会先清理上一次 AI 运营测试数据，再重新插入。
-- ============================================================

SET NAMES utf8mb4;
SET @ops_community_id := 1;

INSERT INTO sys_community (id, name, address, phone, description, create_time, update_time)
SELECT @ops_community_id, '压测演示社区', 'AI运营测试地址', '0755-26050001', 'AI运营周报测试社区', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_community WHERE id = @ops_community_id
);

SET @ops_community_name := (
    SELECT name FROM sys_community WHERE id = @ops_community_id LIMIT 1
);

-- 清理旧测试数据。
DELETE wo
FROM biz_work_order wo
JOIN biz_repair r ON r.id = wo.repair_id
WHERE r.fault_imgs LIKE 'AI_OPS_TEST_%';

DELETE FROM biz_work_order WHERE order_no LIKE 'AI-OPS-%';
DELETE FROM biz_repair WHERE fault_imgs LIKE 'AI_OPS_TEST_%';
DELETE FROM sys_complaint WHERE content LIKE '[AI运营测试]%';
DELETE FROM sys_visitor WHERE visitor_phone LIKE '19926052%';
DELETE FROM sys_fee WHERE remark = 'AI_OPS_TEST';
DELETE FROM sys_notice WHERE title LIKE '[AI运营测试]%';

-- 准备一个稳定的业主和房屋，避免报修外键失败。
INSERT INTO sys_user (username, password, real_name, phone, role, status, community_id, create_time, update_time)
VALUES ('ai_ops_owner_001', '123456', 'AI运营测试业主', '19926051001', 'OWNER', 1, @ops_community_id, NOW(), NOW())
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
(@ops_community_id, @ops_community_name, 'AI1栋', '101', 89.50, 1, 1, 1, '两室一厅', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    community_id = VALUES(community_id),
    area = VALUES(area),
    bind_status = VALUES(bind_status),
    update_time = NOW();
SET @ops_house_id := LAST_INSERT_ID();

-- ============================================================
-- 场景 A：低量平稳周 2026-05-18 至 2026-05-24
-- 预期：少量报修、投诉、访客，风险较低。
-- ============================================================

INSERT INTO biz_repair
(user_id, house_id, fault_type, fault_desc, fault_imgs, status, handle_remark, create_time, update_time, community_id)
VALUES
(@ops_user_id, @ops_house_id, '水管', '[AI运营测试][LOW] AI1栋101厨房水龙头滴水，已预约维修。', 'AI_OPS_TEST_LOW', 'completed', 'AI低量场景：已完成', '2026-05-19 09:15:00', '2026-05-19 11:20:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '公共设施', '[AI运营测试][LOW] AI1栋楼道照明灯不亮，需要更换灯泡。', 'AI_OPS_TEST_LOW', 'pending', 'AI低量场景：待处理', '2026-05-21 18:30:00', '2026-05-21 18:30:00', @ops_community_id);

INSERT INTO sys_complaint
(user_id, community_id, type, content, status, result, create_time, handle_time)
VALUES
(@ops_user_id, @ops_community_id, '环境卫生', '[AI运营测试][LOW] 楼道有少量杂物，希望物业提醒清理。', 'DONE', '已通知保洁处理', '2026-05-20 10:00:00', '2026-05-20 16:00:00');

INSERT INTO sys_visitor
(user_id, community_id, visitor_name, visitor_phone, reason, visit_time, car_no, status, create_time, update_time)
VALUES
(@ops_user_id, @ops_community_id, 'AI低量访客A', '19926052001', '探亲', '2026-05-19 14:00:00', '粤A10001', 'APPROVED', '2026-05-18 20:00:00', '2026-05-18 20:00:00'),
(@ops_user_id, @ops_community_id, 'AI低量访客B', '19926052002', '送货', '2026-05-22 16:30:00', NULL, 'PENDING', '2026-05-22 09:00:00', '2026-05-22 09:00:00');

INSERT INTO sys_notice
(title, content, target_type, community_id, community_name, publish_status, top_flag, publish_time, expire_time, creator_id, create_time, update_time, deleted)
VALUES
('[AI运营测试][LOW] 周末楼道清洁提醒', '本周末将进行楼道清洁，请居民提前收好门口物品。', 'COMMUNITY', @ops_community_id, @ops_community_name, 'PUBLISHED', 0, '2026-05-18 09:00:00', NULL, @ops_user_id, '2026-05-18 08:30:00', '2026-05-18 09:00:00', 0);

-- ============================================================
-- 场景 B：常规运营周 2026-05-25 至 2026-05-31
-- 预期：有一定报修、投诉和访客，AI 会开始总结高频类别和待处理事项。
-- ============================================================

INSERT INTO biz_repair
(user_id, house_id, fault_type, fault_desc, fault_imgs, status, handle_remark, create_time, update_time, community_id)
VALUES
(@ops_user_id, @ops_house_id, '水管', '[AI运营测试][MEDIUM] AI1栋101卫生间下水慢，疑似堵塞。', 'AI_OPS_TEST_MEDIUM', 'completed', 'AI常规场景：已疏通', '2026-05-25 08:20:00', '2026-05-25 13:00:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '水管', '[AI运营测试][MEDIUM] AI2栋1502厨房漏水，水量较小。', 'AI_OPS_TEST_MEDIUM', 'processing', 'AI常规场景：维修中', '2026-05-26 11:10:00', '2026-05-26 12:00:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '电路', '[AI运营测试][MEDIUM] AI3栋走廊插座松动，需要检修。', 'AI_OPS_TEST_MEDIUM', 'completed', 'AI常规场景：已修复', '2026-05-26 15:40:00', '2026-05-26 18:00:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '公共设施', '[AI运营测试][MEDIUM] 小区东门门禁识别慢，早高峰排队。', 'AI_OPS_TEST_MEDIUM', 'pending', 'AI常规场景：待安排', '2026-05-27 08:05:00', '2026-05-27 08:05:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '电梯', '[AI运营测试][MEDIUM] AI5栋电梯运行异响，未出现困人。', 'AI_OPS_TEST_MEDIUM', 'processing', 'AI常规场景：维保检查中', '2026-05-27 19:30:00', '2026-05-27 20:00:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '家电', '[AI运营测试][MEDIUM] 公共活动室空调制冷差。', 'AI_OPS_TEST_MEDIUM', 'completed', 'AI常规场景：已处理', '2026-05-28 14:10:00', '2026-05-28 17:30:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '公共设施', '[AI运营测试][MEDIUM] 地下车库照明局部故障。', 'AI_OPS_TEST_MEDIUM', 'completed', 'AI常规场景：已更换灯具', '2026-05-30 09:20:00', '2026-05-30 14:20:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '水管', '[AI运营测试][MEDIUM] AI6栋水压偏低，需排查阀门。', 'AI_OPS_TEST_MEDIUM', 'completed', 'AI常规场景：已调整阀门', '2026-05-31 10:30:00', '2026-05-31 15:00:00', @ops_community_id);

INSERT INTO sys_complaint
(user_id, community_id, type, content, status, result, create_time, handle_time)
VALUES
(@ops_user_id, @ops_community_id, '停车管理', '[AI运营测试][MEDIUM] 晚上临停车占用固定车位，希望加强巡查。', 'PENDING', NULL, '2026-05-25 20:10:00', NULL),
(@ops_user_id, @ops_community_id, '噪音扰民', '[AI运营测试][MEDIUM] 周末装修声音较大，希望提醒施工时间。', 'DONE', '已联系业主沟通', '2026-05-26 09:20:00', '2026-05-26 14:30:00'),
(@ops_user_id, @ops_community_id, '环境卫生', '[AI运营测试][MEDIUM] 垃圾房异味明显，希望增加清运频次。', 'PENDING', NULL, '2026-05-27 18:40:00', NULL),
(@ops_user_id, @ops_community_id, '门禁管理', '[AI运营测试][MEDIUM] 东门早高峰刷脸速度慢。', 'DONE', '已安排设备检查', '2026-05-28 08:30:00', '2026-05-28 11:00:00'),
(@ops_user_id, @ops_community_id, '设施故障', '[AI运营测试][MEDIUM] 儿童区滑梯扶手松动。', 'DONE', '已加固', '2026-05-29 16:00:00', '2026-05-30 10:00:00');

INSERT INTO sys_visitor
(user_id, community_id, visitor_name, visitor_phone, reason, visit_time, car_no, status, create_time, update_time)
VALUES
(@ops_user_id, @ops_community_id, 'AI常规访客01', '19926052011', '探亲', '2026-05-25 10:00:00', NULL, 'APPROVED', '2026-05-24 18:00:00', '2026-05-24 18:00:00'),
(@ops_user_id, @ops_community_id, 'AI常规访客02', '19926052012', '送货', '2026-05-25 15:00:00', '粤A20002', 'APPROVED', '2026-05-25 09:00:00', '2026-05-25 09:00:00'),
(@ops_user_id, @ops_community_id, 'AI常规访客03', '19926052013', '维修', '2026-05-26 09:30:00', NULL, 'APPROVED', '2026-05-25 20:00:00', '2026-05-25 20:00:00'),
(@ops_user_id, @ops_community_id, 'AI常规访客04', '19926052014', '探亲', '2026-05-27 11:00:00', NULL, 'PENDING', '2026-05-27 08:00:00', '2026-05-27 08:00:00'),
(@ops_user_id, @ops_community_id, 'AI常规访客05', '19926052015', '家政', '2026-05-28 13:30:00', NULL, 'APPROVED', '2026-05-27 18:00:00', '2026-05-27 18:00:00'),
(@ops_user_id, @ops_community_id, 'AI常规访客06', '19926052016', '送货', '2026-05-29 16:00:00', '粤A20006', 'APPROVED', '2026-05-29 09:00:00', '2026-05-29 09:00:00'),
(@ops_user_id, @ops_community_id, 'AI常规访客07', '19926052017', '探亲', '2026-05-30 10:20:00', NULL, 'APPROVED', '2026-05-29 19:00:00', '2026-05-29 19:00:00'),
(@ops_user_id, @ops_community_id, 'AI常规访客08', '19926052018', '朋友来访', '2026-05-30 19:30:00', NULL, 'REJECTED', '2026-05-30 12:00:00', '2026-05-30 12:00:00'),
(@ops_user_id, @ops_community_id, 'AI常规访客09', '19926052019', '送货', '2026-05-31 15:00:00', '粤A20009', 'APPROVED', '2026-05-31 09:00:00', '2026-05-31 09:00:00'),
(@ops_user_id, @ops_community_id, 'AI常规访客10', '19926052020', '探亲', '2026-05-31 20:00:00', NULL, 'PENDING', '2026-05-31 10:00:00', '2026-05-31 10:00:00');

INSERT INTO sys_notice
(title, content, target_type, community_id, community_name, publish_status, top_flag, publish_time, expire_time, creator_id, create_time, update_time, deleted)
VALUES
('[AI运营测试][MEDIUM] 地下车库照明检修通知', '物业将在本周三晚间检修地下车库部分照明，请居民注意绕行。', 'COMMUNITY', @ops_community_id, @ops_community_name, 'PUBLISHED', 0, '2026-05-25 10:00:00', NULL, @ops_user_id, '2026-05-25 09:30:00', '2026-05-25 10:00:00', 0),
('[AI运营测试][MEDIUM] 周末装修施工提醒', '请装修业主遵守施工时间，避免影响邻里休息。', 'COMMUNITY', @ops_community_id, @ops_community_name, 'PUBLISHED', 0, '2026-05-27 09:00:00', NULL, @ops_user_id, '2026-05-27 08:30:00', '2026-05-27 09:00:00', 0),
('[AI运营测试][MEDIUM] 垃圾房清运频次调整', '因天气炎热，物业将临时增加垃圾房清运和消杀频次。', 'COMMUNITY', @ops_community_id, @ops_community_name, 'PUBLISHED', 0, '2026-05-29 11:00:00', NULL, @ops_user_id, '2026-05-29 10:30:00', '2026-05-29 11:00:00', 0);

-- ============================================================
-- 场景 C：高风险高量周 2026-06-01 至 2026-06-07
-- 预期：urgentRepairCount、complaintPending、recentRiskEvents 明显升高，
--      AI 周报应出现风险提醒，manualReviewNeeded 大概率为 true。
-- ============================================================

INSERT INTO biz_repair
(user_id, house_id, fault_type, fault_desc, fault_imgs, status, handle_remark, create_time, update_time, community_id)
VALUES
(@ops_user_id, @ops_house_id, '电路', '[AI运营测试][HIGH] AI1栋101厨房插座漏电，住户反馈有焦糊味。', 'AI_OPS_TEST_HIGH', 'pending', 'AI高风险场景：待人工复核', '2026-06-01 08:10:00', '2026-06-01 08:10:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '水管', '[AI运营测试][HIGH] AI2栋1801厨房爆管，大面积积水已流到客厅。', 'AI_OPS_TEST_HIGH', 'processing', 'AI高风险场景：抢修中', '2026-06-01 09:20:00', '2026-06-01 09:40:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '电梯', '[AI运营测试][HIGH] AI3栋电梯困人，住户按下紧急按钮。', 'AI_OPS_TEST_HIGH', 'processing', 'AI高风险场景：已联系维保', '2026-06-01 18:30:00', '2026-06-01 18:35:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '公共设施', '[AI运营测试][HIGH] 地下车库配电箱冒烟，附近有车辆停放。', 'AI_OPS_TEST_HIGH', 'pending', 'AI高风险场景：待处理', '2026-06-02 07:50:00', '2026-06-02 07:50:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '燃气', '[AI运营测试][HIGH] AI4栋住户反映楼道有燃气异味。', 'AI_OPS_TEST_HIGH', 'processing', 'AI高风险场景：已通知值班人员', '2026-06-02 21:10:00', '2026-06-02 21:20:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '消防', '[AI运营测试][HIGH] 消防通道被杂物堵塞，影响通行。', 'AI_OPS_TEST_HIGH', 'pending', 'AI高风险场景：待清理', '2026-06-03 09:10:00', '2026-06-03 09:10:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '水管', '[AI运营测试][HIGH] AI5栋水井房漏水，地面积水明显。', 'AI_OPS_TEST_HIGH', 'processing', 'AI高风险场景：维修中', '2026-06-03 14:30:00', '2026-06-03 15:00:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '公共设施', '[AI运营测试][HIGH] 小区西门门禁故障，晚间无法进出。', 'AI_OPS_TEST_HIGH', 'pending', 'AI高风险场景：待安排', '2026-06-03 22:40:00', '2026-06-03 22:40:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '电路', '[AI运营测试][HIGH] AI6栋楼道照明线路短路，存在漏电风险。', 'AI_OPS_TEST_HIGH', 'completed', 'AI高风险场景：已处理', '2026-06-04 08:20:00', '2026-06-04 12:00:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '电梯', '[AI运营测试][HIGH] AI7栋电梯多次急停，居民投诉等待时间长。', 'AI_OPS_TEST_HIGH', 'processing', 'AI高风险场景：维保中', '2026-06-04 17:20:00', '2026-06-04 18:00:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '水管', '[AI运营测试][HIGH] AI8栋屋顶水箱溢水，疑似阀门失灵。', 'AI_OPS_TEST_HIGH', 'pending', 'AI高风险场景：待处理', '2026-06-05 10:10:00', '2026-06-05 10:10:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '公共设施', '[AI运营测试][HIGH] 儿童游乐区护栏松动，存在安全隐患。', 'AI_OPS_TEST_HIGH', 'completed', 'AI高风险场景：已围挡', '2026-06-05 16:40:00', '2026-06-05 19:30:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '电路', '[AI运营测试][HIGH] 地下车库多个灯箱冒烟，已临时断电。', 'AI_OPS_TEST_HIGH', 'processing', 'AI高风险场景：抢修中', '2026-06-06 11:20:00', '2026-06-06 11:40:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '水管', '[AI运营测试][HIGH] AI9栋管道漏水，电梯厅出现积水。', 'AI_OPS_TEST_HIGH', 'pending', 'AI高风险场景：待处理', '2026-06-06 20:10:00', '2026-06-06 20:10:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '公共设施', '[AI运营测试][HIGH] 南门道闸无法抬杆，车辆排队明显。', 'AI_OPS_TEST_HIGH', 'completed', 'AI高风险场景：已恢复', '2026-06-07 08:40:00', '2026-06-07 11:00:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '消防', '[AI运营测试][HIGH] 消防栓箱玻璃破损，器材缺失待核查。', 'AI_OPS_TEST_HIGH', 'pending', 'AI高风险场景：待核查', '2026-06-07 15:30:00', '2026-06-07 15:30:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '环境', '[AI运营测试][HIGH] 暴雨后地下车库入口大面积积水。', 'AI_OPS_TEST_HIGH', 'processing', 'AI高风险场景：排水中', '2026-06-07 18:30:00', '2026-06-07 18:40:00', @ops_community_id),
(@ops_user_id, @ops_house_id, '电路', '[AI运营测试][HIGH] AI10栋强电井有异味，需排查起火风险。', 'AI_OPS_TEST_HIGH', 'pending', 'AI高风险场景：待处理', '2026-06-07 21:10:00', '2026-06-07 21:10:00', @ops_community_id);

INSERT INTO sys_complaint
(user_id, community_id, type, content, status, result, create_time, handle_time)
VALUES
(@ops_user_id, @ops_community_id, '安全隐患', '[AI运营测试][HIGH] 多名业主反映地下车库电动车违规充电，担心消防风险。', 'PENDING', NULL, '2026-06-01 10:00:00', NULL),
(@ops_user_id, @ops_community_id, '高空抛物', '[AI运营测试][HIGH] AI2栋疑似高空抛物，楼下公共区域有破碎物。', 'PENDING', NULL, '2026-06-01 16:20:00', NULL),
(@ops_user_id, @ops_community_id, '噪音扰民', '[AI运营测试][HIGH] 夜间施工噪音持续到凌晨，居民情绪较大。', 'PENDING', NULL, '2026-06-02 01:30:00', NULL),
(@ops_user_id, @ops_community_id, '停车管理', '[AI运营测试][HIGH] 消防通道被车辆堵塞，影响救援通行。', 'PENDING', NULL, '2026-06-02 19:30:00', NULL),
(@ops_user_id, @ops_community_id, '环境卫生', '[AI运营测试][HIGH] 暴雨后垃圾房污水外溢，异味明显。', 'PROCESSING', NULL, '2026-06-03 09:10:00', NULL),
(@ops_user_id, @ops_community_id, '门禁管理', '[AI运营测试][HIGH] 西门门禁故障导致陌生人员随意进出。', 'PENDING', NULL, '2026-06-03 23:00:00', NULL),
(@ops_user_id, @ops_community_id, '设施故障', '[AI运营测试][HIGH] 儿童区设施松动，已有家长反馈安全问题。', 'DONE', '已围挡并安排维修', '2026-06-04 12:00:00', '2026-06-04 18:00:00'),
(@ops_user_id, @ops_community_id, '物业费', '[AI运营测试][HIGH] 住户反映物业费明细不清晰，希望解释。', 'PENDING', NULL, '2026-06-05 10:30:00', NULL),
(@ops_user_id, @ops_community_id, '电梯问题', '[AI运营测试][HIGH] 多栋电梯等待时间长，居民集中投诉。', 'PENDING', NULL, '2026-06-06 08:40:00', NULL),
(@ops_user_id, @ops_community_id, '秩序维护', '[AI运营测试][HIGH] 夜间广场聚集喧闹，影响老人休息。', 'DONE', '已加强巡逻', '2026-06-07 22:00:00', '2026-06-07 23:00:00');

INSERT INTO sys_visitor
(user_id, community_id, visitor_name, visitor_phone, reason, visit_time, car_no, status, create_time, update_time)
VALUES
(@ops_user_id, @ops_community_id, 'AI高量访客01', '19926052031', '探亲', '2026-06-01 09:00:00', NULL, 'APPROVED', '2026-05-31 20:00:00', '2026-05-31 20:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客02', '19926052032', '送货', '2026-06-01 11:00:00', '粤A30002', 'APPROVED', '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客03', '19926052033', '维修', '2026-06-01 15:00:00', NULL, 'APPROVED', '2026-06-01 10:00:00', '2026-06-01 10:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客04', '19926052034', '探亲', '2026-06-02 10:00:00', NULL, 'PENDING', '2026-06-02 08:00:00', '2026-06-02 08:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客05', '19926052035', '家政', '2026-06-02 14:00:00', NULL, 'APPROVED', '2026-06-02 09:00:00', '2026-06-02 09:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客06', '19926052036', '送货', '2026-06-03 09:00:00', '粤A30006', 'APPROVED', '2026-06-03 08:00:00', '2026-06-03 08:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客07', '19926052037', '探亲', '2026-06-03 19:00:00', NULL, 'APPROVED', '2026-06-03 12:00:00', '2026-06-03 12:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客08', '19926052038', '朋友来访', '2026-06-04 10:20:00', NULL, 'REJECTED', '2026-06-04 08:00:00', '2026-06-04 08:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客09', '19926052039', '送货', '2026-06-04 15:30:00', '粤A30009', 'APPROVED', '2026-06-04 09:00:00', '2026-06-04 09:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客10', '19926052040', '探亲', '2026-06-05 11:00:00', NULL, 'PENDING', '2026-06-05 09:00:00', '2026-06-05 09:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客11', '19926052041', '装修', '2026-06-05 14:30:00', NULL, 'APPROVED', '2026-06-05 10:00:00', '2026-06-05 10:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客12', '19926052042', '外卖', '2026-06-05 20:00:00', NULL, 'APPROVED', '2026-06-05 18:00:00', '2026-06-05 18:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客13', '19926052043', '探亲', '2026-06-06 09:30:00', NULL, 'APPROVED', '2026-06-05 20:00:00', '2026-06-05 20:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客14', '19926052044', '送货', '2026-06-06 13:20:00', '粤A30014', 'APPROVED', '2026-06-06 09:00:00', '2026-06-06 09:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客15', '19926052045', '维修', '2026-06-06 16:40:00', NULL, 'APPROVED', '2026-06-06 12:00:00', '2026-06-06 12:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客16', '19926052046', '探亲', '2026-06-07 10:00:00', NULL, 'APPROVED', '2026-06-07 08:00:00', '2026-06-07 08:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客17', '19926052047', '送货', '2026-06-07 15:30:00', '粤A30017', 'APPROVED', '2026-06-07 09:00:00', '2026-06-07 09:00:00'),
(@ops_user_id, @ops_community_id, 'AI高量访客18', '19926052048', '朋友来访', '2026-06-07 21:00:00', NULL, 'PENDING', '2026-06-07 18:00:00', '2026-06-07 18:00:00');

INSERT INTO sys_notice
(title, content, target_type, community_id, community_name, publish_status, top_flag, publish_time, expire_time, creator_id, create_time, update_time, deleted)
VALUES
('[AI运营测试][HIGH] 暴雨天气安全提醒', '预计本周有强降雨，请居民注意地下车库积水和用电安全。', 'COMMUNITY', @ops_community_id, @ops_community_name, 'PUBLISHED', 1, '2026-06-01 08:00:00', NULL, @ops_user_id, '2026-06-01 07:30:00', '2026-06-01 08:00:00', 0),
('[AI运营测试][HIGH] 消防通道专项整治通知', '物业将集中清理消防通道杂物和违规停放车辆，请居民配合。', 'COMMUNITY', @ops_community_id, @ops_community_name, 'PUBLISHED', 1, '2026-06-03 09:00:00', NULL, @ops_user_id, '2026-06-03 08:30:00', '2026-06-03 09:00:00', 0),
('[AI运营测试][HIGH] 电动车充电安全提示', '严禁电动车进楼入户充电，请统一停放至集中充电区。', 'COMMUNITY', @ops_community_id, @ops_community_name, 'PUBLISHED', 1, '2026-06-05 10:00:00', NULL, @ops_user_id, '2026-06-05 09:30:00', '2026-06-05 10:00:00', 0),
('[AI运营测试][HIGH] 电梯维保进度说明', '针对多栋电梯运行异常，物业已联系维保单位逐栋排查。', 'COMMUNITY', @ops_community_id, @ops_community_name, 'PUBLISHED', 0, '2026-06-07 18:00:00', NULL, @ops_user_id, '2026-06-07 17:30:00', '2026-06-07 18:00:00', 0);

-- 当前欠费属于“当前存量指标”，不是按周过滤。三个日期区间都会看到这些未缴费账单。
INSERT INTO sys_fee
(house_id, community_id, building_no, fee_cycle, fee_amount, fee_type, status, remind_count, due_date, create_time, update_time, remark)
VALUES
(@ops_house_id, @ops_community_id, 'AI1栋', '2026-05', 260.00, '物业费', 'UNPAID', 1, '2026-05-31 23:59:59', '2026-05-01 09:00:00', '2026-05-20 09:00:00', 'AI_OPS_TEST'),
(@ops_house_id, @ops_community_id, 'AI1栋', '2026-06', 260.00, '物业费', 'UNPAID', 0, '2026-06-30 23:59:59', '2026-06-01 09:00:00', '2026-06-01 09:00:00', 'AI_OPS_TEST'),
(@ops_house_id, @ops_community_id, 'AI2栋', '2026-05', 310.00, '物业费', 'UNPAID', 2, '2026-05-31 23:59:59', '2026-05-01 09:00:00', '2026-05-25 09:00:00', 'AI_OPS_TEST'),
(@ops_house_id, @ops_community_id, 'AI3栋', '2026-05', 180.00, '物业费', 'PAYING', 1, '2026-05-31 23:59:59', '2026-05-01 09:00:00', '2026-05-26 09:00:00', 'AI_OPS_TEST'),
(@ops_house_id, @ops_community_id, 'AI4栋', '2026-05', 420.00, '物业费', 'OVERDUE', 3, '2026-05-31 23:59:59', '2026-05-01 09:00:00', '2026-06-03 09:00:00', 'AI_OPS_TEST'),
(@ops_house_id, @ops_community_id, 'AI5栋', '2026-05', 220.00, '物业费', 'UNPAID', 1, '2026-05-31 23:59:59', '2026-05-01 09:00:00', '2026-06-04 09:00:00', 'AI_OPS_TEST'),
(@ops_house_id, @ops_community_id, 'AI6栋', '2026-06', 260.00, '物业费', 'UNPAID', 0, '2026-06-30 23:59:59', '2026-06-01 09:00:00', '2026-06-05 09:00:00', 'AI_OPS_TEST'),
(@ops_house_id, @ops_community_id, 'AI7栋', '2026-06', 280.00, '物业费', 'PAID', 0, '2026-06-30 23:59:59', '2026-06-01 09:00:00', '2026-06-06 09:00:00', 'AI_OPS_TEST');

-- 给所有 AI 测试报修生成对应工单，用于 urgentRepairCount 和风险判断。
INSERT INTO biz_work_order
(repair_id, order_no, community_id, worker_id, worker_name, worker_phone, status, priority, create_time, update_time)
SELECT
    r.id,
    CONCAT('AI-OPS-', r.id),
    r.community_id,
    NULL,
    NULL,
    NULL,
    CASE LOWER(r.status)
        WHEN 'completed' THEN 'COMPLETED'
        WHEN 'processing' THEN 'PROCESSING'
        ELSE 'PENDING'
    END,
    CASE
        WHEN r.fault_imgs = 'AI_OPS_TEST_HIGH' THEN 3
        WHEN r.fault_imgs = 'AI_OPS_TEST_MEDIUM'
             AND (r.fault_desc LIKE '%漏水%' OR r.fault_desc LIKE '%电梯%' OR r.fault_desc LIKE '%门禁%') THEN 2
        ELSE 1
    END,
    r.create_time,
    r.update_time
FROM biz_repair r
WHERE r.fault_imgs LIKE 'AI_OPS_TEST_%';

SELECT
    'AI operations weekly report test data inserted' AS message,
    @ops_community_id AS communityId,
    @ops_community_name AS communityName,
    @ops_user_id AS userId,
    @ops_house_id AS houseId;
