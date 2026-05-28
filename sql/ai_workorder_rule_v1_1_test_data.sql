SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- AI work order rule-v1.1 test seed data.
-- This script creates one stable community/user/house set, then inserts repair reports
-- that exercise category, urgency, location extraction, multi-team review, and fallback.

INSERT INTO `sys_community` (`name`, `address`, `phone`, `description`, `create_time`, `update_time`)
VALUES ('AI测试小区', 'AI测试路1号', '13800009999', 'AI工单助手测试社区', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `address` = VALUES(`address`),
  `phone` = VALUES(`phone`),
  `description` = VALUES(`description`),
  `update_time` = NOW();

SELECT `id` INTO @ai_test_community_id
FROM `sys_community`
WHERE `name` = 'AI测试小区'
LIMIT 1;

INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `role`, `status`, `community_id`, `create_time`, `update_time`)
VALUES ('ai_test_owner', '123456', 'AI测试业主', '13999009999', 'owner', 1, @ai_test_community_id, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `real_name` = VALUES(`real_name`),
  `role` = VALUES(`role`),
  `status` = VALUES(`status`),
  `community_id` = VALUES(`community_id`),
  `update_time` = NOW();

SELECT `id` INTO @ai_test_user_id
FROM `sys_user`
WHERE `username` = 'ai_test_owner'
LIMIT 1;

INSERT INTO `sys_house` (`community_id`, `community_name`, `building_no`, `house_no`, `area`, `is_default`, `bind_status`, `floor`, `type`, `create_time`, `update_time`)
VALUES (@ai_test_community_id, 'AI测试小区', '3栋', '2单元1801', 108.00, 1, 1, 18, '三室两厅', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `community_id` = VALUES(`community_id`),
  `area` = VALUES(`area`),
  `is_default` = VALUES(`is_default`),
  `bind_status` = VALUES(`bind_status`),
  `floor` = VALUES(`floor`),
  `type` = VALUES(`type`),
  `update_time` = NOW();

SELECT `id` INTO @ai_test_house_id
FROM `sys_house`
WHERE `community_name` = 'AI测试小区'
  AND `building_no` = '3栋'
  AND `house_no` = '2单元1801'
LIMIT 1;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `biz_repair`
  (`user_id`, `house_id`, `fault_type`, `fault_desc`, `fault_imgs`, `status`, `handle_remark`, `community_id`, `create_time`, `update_time`)
VALUES
  (@ai_test_user_id, @ai_test_house_id, '水管',
   '[AI_TEST_RULE_V1_1] 3栋2单元1801厨房水槽下方漏水，水已经流到客厅。',
   '', 'pending', NULL, @ai_test_community_id, NOW(), NOW()),

  (@ai_test_user_id, @ai_test_house_id, '电路',
   '[AI_TEST_RULE_V1_1] 卫生间漏水，插座附近疑似漏电，还有火花。',
   '', 'pending', NULL, @ai_test_community_id, NOW(), NOW()),

  (@ai_test_user_id, @ai_test_house_id, '电梯',
   '[AI_TEST_RULE_V1_1] 电梯困人，住户被困在轿厢里，现场有冒烟。',
   '', 'pending', NULL, @ai_test_community_id, NOW(), NOW()),

  (@ai_test_user_id, @ai_test_house_id, '公共设施',
   '[AI_TEST_RULE_V1_1] 地下车库B区电梯口有积水，影响通行。',
   '', 'pending', NULL, @ai_test_community_id, NOW(), NOW()),

  (@ai_test_user_id, @ai_test_house_id, '公共设施',
   '[AI_TEST_RULE_V1_1] 西门门禁打不开，老人无法进出。',
   '', 'pending', NULL, @ai_test_community_id, NOW(), NOW()),

  (@ai_test_user_id, @ai_test_house_id, '其他',
   '[AI_TEST_RULE_V1_1] 住户说家里有点问题，但不确定是什么，需要物业联系确认。',
   '', 'pending', NULL, @ai_test_community_id, NOW(), NOW());

SELECT
  `id`,
  `fault_type`,
  `fault_desc`,
  `status`,
  `community_id`,
  `create_time`
FROM `biz_repair`
WHERE `fault_desc` LIKE '[AI_TEST_RULE_V1_1]%'
ORDER BY `id` DESC;
