USE `smart_community`;

CREATE TABLE IF NOT EXISTS `sys_house_bind_request` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '申请用户ID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户名',
    `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '真实姓名',
    `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
    `house_id` bigint NOT NULL COMMENT '房屋ID',
    `community_id` bigint DEFAULT NULL COMMENT '社区ID',
    `community_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '社区名称',
    `building_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '楼栋号',
    `house_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '房号',
    `identity_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '身份类型: OWNER/FAMILY/TENANT',
    `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '审核状态: PENDING/APPROVED/REJECTED',
    `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `approve_time` datetime DEFAULT NULL COMMENT '审核时间',
    `approve_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '驳回原因',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_house_bind_user_id` (`user_id` ASC) USING BTREE,
    INDEX `idx_house_bind_house_id` (`house_id` ASC) USING BTREE,
    INDEX `idx_house_bind_community_id` (`community_id` ASC) USING BTREE,
    INDEX `idx_house_bind_status` (`status` ASC) USING BTREE,
    INDEX `idx_house_bind_apply_time` (`apply_time` ASC) USING BTREE,
    INDEX `idx_house_bind_house_status` (`house_id` ASC, `status` ASC) USING BTREE,
    INDEX `idx_house_bind_user_house_status` (`user_id` ASC, `house_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '房屋绑定申请表'
  ROW_FORMAT = DYNAMIC;
