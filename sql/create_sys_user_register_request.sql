USE `smart_community`;

CREATE TABLE IF NOT EXISTS `sys_user_register_request` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录用户名',
    `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '加密后的密码',
    `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '真实姓名',
    `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
    `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请角色: owner/admin/super_admin/worker',
    `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '申请状态: PENDING/APPROVED/REJECTED/ACTIVE',
    `community_id` bigint DEFAULT NULL COMMENT '归属社区ID',
    `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `approve_time` datetime DEFAULT NULL COMMENT '审核时间',
    `approve_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '驳回原因',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_register_request_username` (`username` ASC) USING BTREE,
    INDEX `idx_register_request_status` (`status` ASC) USING BTREE,
    INDEX `idx_register_request_role` (`role` ASC) USING BTREE,
    INDEX `idx_register_request_phone_status` (`phone` ASC, `status` ASC) USING BTREE,
    INDEX `idx_register_request_apply_time` (`apply_time` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户注册申请表'
  ROW_FORMAT = DYNAMIC;
