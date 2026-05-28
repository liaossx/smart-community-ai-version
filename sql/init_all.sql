/*
 Navicat Premium Dump SQL

 Source Server         : 192.168.10.101_3306
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : 192.168.10.101:3306
 Source Schema         : smart_community

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 28/04/2026 16:23:02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for biz_express
-- ----------------------------
DROP TABLE IF EXISTS `biz_express`;
CREATE TABLE `biz_express`  (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `community_id` bigint NULL DEFAULT NULL COMMENT '归属社区ID',
                                `user_id` bigint NULL DEFAULT NULL COMMENT '关联业主ID',
                                `house_id` bigint NULL DEFAULT NULL COMMENT '关联房屋ID',
                                `recipient_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收件人姓名',
                                `recipient_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收件人电话',
                                `company` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '快递公司',
                                `tracking_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '运单号',
                                `location_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '存放位置编码（柜子/货架）',
                                `pickup_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '取件码',
                                `status` enum('WAITING','PICKED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'WAITING' COMMENT '快递状态（WAITING待取件/PICKED已取件）',
                                `authorized` tinyint(1) NULL DEFAULT 0 COMMENT '是否已授权代取',
                                `authorized_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '授权人姓名',
                                `authorized_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '授权人电话',
                                `authorize_expire_time` datetime NULL DEFAULT NULL COMMENT '授权有效期',
                                `pickup_time` datetime NULL DEFAULT NULL COMMENT '实际取件时间',
                                `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注信息',
                                `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`) USING BTREE,
                                INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '业主ID索引',
                                INDEX `idx_house_id`(`house_id` ASC) USING BTREE COMMENT '房屋ID索引',
                                INDEX `idx_tracking_no`(`tracking_no` ASC) USING BTREE COMMENT '运单号索引',
                                INDEX `idx_status`(`status` ASC) USING BTREE COMMENT '快递状态索引',
                                INDEX `idx_pickup_code`(`pickup_code` ASC) USING BTREE COMMENT '取件码索引',
                                INDEX `idx_express_community`(`community_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '快递信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_group_activity
-- ----------------------------
DROP TABLE IF EXISTS `biz_group_activity`;
CREATE TABLE `biz_group_activity`  (
                                       `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `community_id` bigint NULL DEFAULT NULL COMMENT '归属社区ID',
                                       `sponsor_id` bigint UNSIGNED NOT NULL COMMENT '发起人ID',
                                       `subject` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '拼团主题',
                                       `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '拼团说明',
                                       `target_count` int UNSIGNED NOT NULL COMMENT '目标人数',
                                       `joined_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '已参团人数',
                                       `deadline` datetime NULL DEFAULT NULL COMMENT '截止时间',
                                       `status` enum('ONGOING','SUCCESS','FAILED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ONGOING' COMMENT '拼团状态',
                                       `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '补充备注',
                                       `finish_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       PRIMARY KEY (`id`) USING BTREE,
                                       INDEX `idx_group_activity_status`(`status` ASC) USING BTREE,
                                       INDEX `idx_group_activity_deadline`(`deadline` ASC) USING BTREE,
                                       INDEX `idx_group_community`(`community_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_group_member
-- ----------------------------
DROP TABLE IF EXISTS `biz_group_member`;
CREATE TABLE `biz_group_member`  (
                                     `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `group_id` bigint UNSIGNED NOT NULL COMMENT '拼团ID',
                                     `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
                                     `role` enum('SPONSOR','MEMBER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色',
                                     `status` enum('JOINED','COMPLETED','FAILED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'JOINED' COMMENT '成员状态',
                                     `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
                                     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE INDEX `uk_group_user`(`group_id` ASC, `user_id` ASC) USING BTREE,
                                     INDEX `idx_group_member_status`(`status` ASC) USING BTREE,
                                     CONSTRAINT `fk_group_member_activity` FOREIGN KEY (`group_id`) REFERENCES `biz_group_activity` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_account
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_account`;
CREATE TABLE `biz_parking_account`  (
                                        `id` bigint NOT NULL AUTO_INCREMENT,
                                        `user_id` bigint NOT NULL COMMENT '用户ID',
                                        `balance` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
                                        `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / FROZEN',
                                        `create_time` datetime NOT NULL,
                                        `update_time` datetime NOT NULL,
                                        PRIMARY KEY (`id`) USING BTREE,
                                        UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_account_log
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_account_log`;
CREATE TABLE `biz_parking_account_log`  (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `account_id` bigint NOT NULL COMMENT '账户ID',
                                            `order_id` bigint NULL DEFAULT NULL COMMENT '关联订单',
                                            `amount` decimal(10, 2) NOT NULL COMMENT '变动金额',
                                            `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'RECHARGE / CONSUME / REFUND',
                                            `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                            `create_time` datetime NOT NULL,
                                            PRIMARY KEY (`id`) USING BTREE,
                                            INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
                                            INDEX `idx_order_id`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_authorize
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_authorize`;
CREATE TABLE `biz_parking_authorize`  (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `space_id` bigint NOT NULL COMMENT '车位ID',
                                          `user_id` bigint NOT NULL COMMENT '用户ID',
                                          `authorized_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '授权人姓名',
                                          `authorized_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '授权人手机号',
                                          `plate_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车牌号',
                                          `start_time` datetime NOT NULL COMMENT '授权开始时间',
                                          `end_time` datetime NULL DEFAULT NULL COMMENT '授权结束时间',
                                          `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE（激活）/ EXPIRED（已过期）/ REVOKED（已撤销）',
                                          `create_time` datetime NOT NULL COMMENT '创建时间',
                                          `update_time` datetime NOT NULL COMMENT '更新时间',
                                          PRIMARY KEY (`id`) USING BTREE,
                                          INDEX `idx_space_id`(`space_id` ASC) USING BTREE,
                                          INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                                          INDEX `idx_plate_no`(`plate_no` ASC) USING BTREE,
                                          INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '车位授权表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_gate_log
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_gate_log`;
CREATE TABLE `biz_parking_gate_log`  (
                                         `id` bigint NOT NULL AUTO_INCREMENT,
                                         `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID（外来车辆为空）',
                                         `space_id` bigint NULL DEFAULT NULL COMMENT '车位ID（外来车辆入闸时为空）',
                                         `gate_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开闸类型 FIXED / TEMP',
                                         `action` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'OPEN / CLOSE',
                                         `result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'SUCCESS / FAIL',
                                         `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                         `create_time` datetime NOT NULL,
                                         `plate_no` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车牌号',
                                         PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_lease_order
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_lease_order`;
CREATE TABLE `biz_parking_lease_order`  (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `user_id` bigint NOT NULL,
                                            `space_id` bigint NOT NULL,
                                            `lease_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                            `amount` decimal(10, 2) NOT NULL,
                                            `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UNPAID',
                                            `pay_time` datetime NULL DEFAULT NULL,
                                            `create_time` datetime NOT NULL,
                                            `update_time` datetime NOT NULL,
                                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_order
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_order`;
CREATE TABLE `biz_parking_order`  (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `community_id` bigint NULL DEFAULT NULL COMMENT '归属社区ID',
                                      `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
                                      `user_id` bigint NOT NULL COMMENT '用户ID',
                                      `space_id` bigint NOT NULL COMMENT '车位ID',
                                      `order_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单类型：TEMP（临时）/ FIXED（固定）',
                                      `amount` decimal(10, 2) NOT NULL COMMENT '订单金额',
                                      `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UNPAID' COMMENT '状态：UNPAID（未支付）/ PAID（已支付）/ CANCELLED（已取消）',
                                      `start_time` datetime NOT NULL COMMENT '停车开始时间',
                                      `end_time` datetime NULL DEFAULT NULL COMMENT '停车结束时间',
                                      `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
                                      `pay_channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付渠道',
                                      `pay_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付备注',
                                      `create_time` datetime NOT NULL COMMENT '创建时间',
                                      `update_time` datetime NOT NULL COMMENT '更新时间',
                                      `renew_month` int NULL DEFAULT 1 COMMENT '续费月份',
                                      `plate_no` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '车牌号',
                                      `trade_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付流水号',
                                      `paid_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '实付金额',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
                                      UNIQUE INDEX `uk_parking_trade_no`(`trade_no` ASC) USING BTREE,
                                      INDEX `idx_order_no`(`order_no` ASC) USING BTREE,
                                      INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                                      INDEX `idx_space_id`(`space_id` ASC) USING BTREE,
                                      INDEX `idx_status`(`status` ASC) USING BTREE,
                                      INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
                                      INDEX `idx_parking_order_community`(`community_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 45 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '停车订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_reserve
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_reserve`;
CREATE TABLE `biz_parking_reserve`  (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `user_id` bigint NOT NULL COMMENT '用户ID',
                                        `space_id` bigint NOT NULL COMMENT '车位ID',
                                        `reserve_start_time` datetime NOT NULL COMMENT '预约开始时间',
                                        `reserve_end_time` datetime NOT NULL COMMENT '预约结束时间',
                                        `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'RESERVED' COMMENT '状态：RESERVED(已预约)/EXPIRED(已过期)/CANCELLED(已取消)',
                                        `cancel_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '取消原因',
                                        `cancel_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '取消人类型',
                                        `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '车位预约记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_space
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_space`;
CREATE TABLE `biz_parking_space`  (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `community_id` bigint NULL DEFAULT NULL,
                                      `community_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                      `space_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车位编号',
                                      `space_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车位类型：TEMP（临时）/ FIXED（固定）',
                                      `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE（可用）/ OCCUPIED（已占用）/ RESERVED（已预订）',
                                      `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0未删除，1已删除',
                                      `create_time` datetime NOT NULL COMMENT '创建时间',
                                      `update_time` datetime NOT NULL COMMENT '更新时间',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      INDEX `idx_community_id`(`community_id` ASC) USING BTREE,
                                      INDEX `idx_space_no`(`space_no` ASC) USING BTREE,
                                      INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 381 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '车位信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_space_lease
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_space_lease`;
CREATE TABLE `biz_parking_space_lease`  (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `space_id` bigint NOT NULL COMMENT '车位ID',
                                            `user_id` bigint NOT NULL COMMENT '使用人ID',
                                            `house_id` bigint NULL DEFAULT NULL COMMENT '房屋ID',
                                            `lease_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'MONTHLY / YEARLY / PERPETUAL',
                                            `start_time` datetime NOT NULL,
                                            `end_time` datetime NULL DEFAULT NULL COMMENT '永久车位为 NULL',
                                            `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ACTIVE / EXPIRED / TERMINATED',
                                            `source_order_id` bigint NULL DEFAULT NULL COMMENT '来源订单ID',
                                            `create_time` datetime NOT NULL,
                                            `update_time` datetime NOT NULL,
                                            PRIMARY KEY (`id`) USING BTREE,
                                            INDEX `idx_space_user`(`space_id` ASC, `user_id` ASC) USING BTREE,
                                            INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '车位使用权表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_parking_space_plate
-- ----------------------------
DROP TABLE IF EXISTS `biz_parking_space_plate`;
CREATE TABLE `biz_parking_space_plate`  (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `space_id` bigint NOT NULL COMMENT '车位ID',
                                            `plate_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车牌号',
                                            `user_id` bigint NOT NULL COMMENT '所属用户',
                                            `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ACTIVE / DISABLED',
                                            `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拒绝原因',
                                            `create_time` datetime NOT NULL,
                                            `update_time` datetime NOT NULL,
                                            PRIMARY KEY (`id`) USING BTREE,
                                            UNIQUE INDEX `uk_space_plate`(`space_id` ASC, `plate_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_repair
-- ----------------------------
DROP TABLE IF EXISTS `biz_repair`;
CREATE TABLE `biz_repair`  (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报修ID（自增）',
                               `user_id` bigint NOT NULL COMMENT '关联的业主ID（对应sys_user.id）',
                               `house_id` bigint NOT NULL COMMENT '关联的房屋ID（对应sys_house.id）',
                               `fault_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '故障类型（下拉选择：水管、电路、家电、公共设施）',
                               `fault_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '故障描述（业主填写）',
                               `fault_imgs` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '故障图片URL（多张用逗号分隔，如https://xxx1.jpg,https://xxx2.jpg）',
                               `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '报修状态：pending（待处理）、processing（处理中）、completed（已完成）',
                               `handle_remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '处理备注（管理员更新状态时填写，如\"师傅已接单\"）',
                               `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
                               `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `community_id` bigint NULL DEFAULT NULL COMMENT '所属社区ID',
                               PRIMARY KEY (`id`) USING BTREE,
                               INDEX `house_id`(`house_id` ASC) USING BTREE,
                               INDEX `idx_status`(`status` ASC) USING BTREE,
                               INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                               INDEX `idx_community_id`(`community_id` ASC) USING BTREE,
                               CONSTRAINT `biz_repair_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                               CONSTRAINT `biz_repair_ibfk_2` FOREIGN KEY (`house_id`) REFERENCES `sys_house` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                               CONSTRAINT `fk_repair_community` FOREIGN KEY (`community_id`) REFERENCES `sys_community` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '报修表（核心业务表）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_topic
-- ----------------------------
DROP TABLE IF EXISTS `biz_topic`;
CREATE TABLE `biz_topic`  (
                              `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                              `community_id` bigint NULL DEFAULT NULL COMMENT '归属社区ID',
                              `user_id` bigint UNSIGNED NOT NULL COMMENT '发帖人ID',
                              `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
                              `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '正文',
                              `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图片URL（逗号分隔）',
                              `status` enum('PENDING','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
                              `like_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
                              `comment_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数',
                              `audit_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '审核人ID',
                              `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注',
                              `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
                              `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览数',
                              PRIMARY KEY (`id`) USING BTREE,
                              INDEX `idx_topic_user`(`user_id` ASC) USING BTREE,
                              INDEX `idx_topic_status`(`status` ASC) USING BTREE,
                              INDEX `idx_topic_deleted`(`deleted` ASC) USING BTREE,
                              INDEX `idx_topic_community`(`community_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区话题' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_topic_comment
-- ----------------------------
DROP TABLE IF EXISTS `biz_topic_comment`;
CREATE TABLE `biz_topic_comment`  (
                                      `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
                                      `topic_id` bigint UNSIGNED NOT NULL COMMENT '话题ID',
                                      `user_id` bigint UNSIGNED NOT NULL COMMENT '评论人ID',
                                      `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
                                      `parent_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '父评论ID',
                                      `root_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '根评论ID',
                                      `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`id`) USING BTREE,
                                      INDEX `idx_topic`(`topic_id` ASC) USING BTREE,
                                      INDEX `idx_parent`(`parent_id` ASC) USING BTREE,
                                      INDEX `idx_root`(`root_id` ASC) USING BTREE,
                                      CONSTRAINT `fk_topic_comment_topic` FOREIGN KEY (`topic_id`) REFERENCES `biz_topic` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '话题评论' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_topic_like
-- ----------------------------
DROP TABLE IF EXISTS `biz_topic_like`;
CREATE TABLE `biz_topic_like`  (
                                   `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `topic_id` bigint UNSIGNED NOT NULL COMMENT '话题ID',
                                   `user_id` bigint UNSIGNED NOT NULL COMMENT '点赞人ID',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE INDEX `uk_topic_like`(`topic_id` ASC, `user_id` ASC) USING BTREE,
                                   CONSTRAINT `fk_topic_like_topic` FOREIGN KEY (`topic_id`) REFERENCES `biz_topic` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '话题点赞' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_vehicle
-- ----------------------------
DROP TABLE IF EXISTS `biz_vehicle`;
CREATE TABLE `biz_vehicle`  (
                                `id` bigint NOT NULL AUTO_INCREMENT,
                                `community_id` bigint NULL DEFAULT NULL COMMENT '归属社区ID',
                                `plate_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车牌号',
                                `user_id` bigint NOT NULL COMMENT '车主用户ID',
                                `brand` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '车辆品牌',
                                `vehicle_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'CAR' COMMENT 'CAR / SUV / EV',
                                `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
                                `create_time` datetime NOT NULL,
                                `update_time` datetime NOT NULL,
                                `color` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '颜色',
                                PRIMARY KEY (`id`) USING BTREE,
                                UNIQUE INDEX `uk_plate_no`(`plate_no` ASC) USING BTREE,
                                INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                                INDEX `idx_vehicle_community`(`community_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '车辆表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_work_order
-- ----------------------------
DROP TABLE IF EXISTS `biz_work_order`;
CREATE TABLE `biz_work_order`  (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `repair_id` bigint NOT NULL COMMENT '关联报修单ID',
                                   `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工单编号',
                                   `community_id` bigint NULL DEFAULT NULL COMMENT '所属社区ID',
                                   `worker_id` bigint NULL DEFAULT NULL COMMENT '维修员ID',
                                   `worker_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '维修员姓名',
                                   `worker_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '维修员电话',
                                   `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待指派, ASSIGNED-已指派, PROCESSING-处理中, COMPLETED-已完成, CANCELLED-已取消',
                                   `priority` tinyint NULL DEFAULT 1 COMMENT '优先级: 1-普通, 2-紧急, 3-特急',
                                   `plan_start_time` datetime NULL DEFAULT NULL COMMENT '计划开始时间',
                                   `actual_start_time` datetime NULL DEFAULT NULL COMMENT '实际开始时间',
                                   `actual_end_time` datetime NULL DEFAULT NULL COMMENT '实际结束时间',
                                   `process_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '处理结果',
                                   `process_imgs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '处理后图片',
                                   `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
                                   INDEX `idx_repair_id`(`repair_id` ASC) USING BTREE,
                                   INDEX `idx_worker_id`(`worker_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_activity
-- ----------------------------
DROP TABLE IF EXISTS `sys_activity`;
CREATE TABLE `sys_activity`  (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `community_id` bigint NULL DEFAULT NULL,
                                 `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                 `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
                                 `start_time` datetime NULL DEFAULT NULL,
                                 `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                 `max_count` int NULL DEFAULT NULL,
                                 `signup_count` int NULL DEFAULT 0,
                                 `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                 `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                 `create_time` datetime NULL DEFAULT NULL,
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区活动' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_activity_signup
-- ----------------------------
DROP TABLE IF EXISTS `sys_activity_signup`;
CREATE TABLE `sys_activity_signup`  (
                                        `id` bigint NOT NULL AUTO_INCREMENT,
                                        `activity_id` bigint NOT NULL,
                                        `user_id` bigint NOT NULL,
                                        `signup_time` datetime NULL DEFAULT NULL,
                                        PRIMARY KEY (`id`) USING BTREE,
                                        UNIQUE INDEX `uk_activity_user`(`activity_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '活动报名' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_community
-- ----------------------------
DROP TABLE IF EXISTS `sys_community`;
CREATE TABLE `sys_community`  (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '社区ID',
                                  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '社区名称',
                                  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
                                  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
                                  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '简介',
                                  `total_houses` int NULL DEFAULT 0 COMMENT '总户数',
                                  `total_parkings` int NULL DEFAULT 0 COMMENT '车位总数',
                                  `green_rate` double NULL DEFAULT 0 COMMENT '绿化率',
                                  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  UNIQUE INDEX `idx_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_complaint
-- ----------------------------
DROP TABLE IF EXISTS `sys_complaint`;
CREATE TABLE `sys_complaint`  (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `user_id` bigint NOT NULL,
                                  `community_id` bigint NULL DEFAULT NULL,
                                  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
                                  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
                                  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
                                  `create_time` datetime NULL DEFAULT NULL,
                                  `handle_time` datetime NULL DEFAULT NULL,
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '投诉建议' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
                               `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数主键',
                               `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数名称',
                               `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数键名',
                               `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数键值',
                               `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
                               `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
                               `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                               PRIMARY KEY (`config_id`) USING BTREE,
                               UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 105 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '参数配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_fee
-- ----------------------------
DROP TABLE IF EXISTS `sys_fee`;
CREATE TABLE `sys_fee`  (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `house_id` bigint NOT NULL COMMENT '房屋ID',
                            `community_id` bigint NULL DEFAULT NULL COMMENT '社区ID',
                            `building_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '楼栋号',
                            `fee_cycle` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '费用周期(2025-01)',
                            `fee_amount` decimal(10, 2) NOT NULL COMMENT '金额',
                            `fee_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '物业费',
                            `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'UNPAID' COMMENT 'UNPAID=待缴, PAID=已缴',
                            `remind_count` int NULL DEFAULT 0 COMMENT '催缴次数',
                            `due_date` datetime NULL DEFAULT NULL COMMENT '截止时间',
                            `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
                            `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物业费账单' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_fee_record
-- ----------------------------
DROP TABLE IF EXISTS `sys_fee_record`;
CREATE TABLE `sys_fee_record`  (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `fee_id` bigint NOT NULL COMMENT '账单ID',
                                   `user_id` bigint NOT NULL COMMENT '缴费人ID',
                                   `house_id` bigint NOT NULL COMMENT '房屋ID',
                                   `pay_amount` decimal(10, 2) NOT NULL COMMENT '实际缴费金额',
                                   `pay_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付方式（WECHAT/ALIPAY/CASH）',
                                   `pay_time` datetime NULL DEFAULT NULL COMMENT '缴费时间',
                                   `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付订单号',
                                   `trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付交易号',
                                   `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'FAIL' COMMENT '缴费状态（SUCCESS-成功、FAIL-失败、REFUND-退款）',
                                   `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   INDEX `idx_fee_id`(`fee_id` ASC) USING BTREE,
                                   INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                                   INDEX `idx_order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 199 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '缴费记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_house
-- ----------------------------
DROP TABLE IF EXISTS `sys_house`;
CREATE TABLE `sys_house`  (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房屋ID（自增）',
                              `community_id` bigint NULL DEFAULT NULL COMMENT '归属社区ID',
                              `community_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '小区名称（如阳光小区）',
                              `building_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '楼栋号（如1栋、1-2栋）',
                              `house_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '房屋编号（如101、2-302）',
                              `area` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '房屋面积（单位：㎡）',
                              `is_default` tinyint NULL DEFAULT 1 COMMENT '是否默认房屋：1（是）、0（否）（业主默认用这套房屋报修）',
                              `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
                              `bind_status` tinyint NOT NULL DEFAULT 0 COMMENT '房屋绑定状态:0=未绑定，1=绑定',
                              `floor` int NULL DEFAULT NULL COMMENT '楼层',
                              `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '户型',
                              `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`id`) USING BTREE,
                              UNIQUE INDEX `idx_user_house`(`community_name` ASC, `building_no` ASC, `house_no` ASC) USING BTREE,
                              INDEX `idx_house_community`(`community_id` ASC) USING BTREE,
                              CONSTRAINT `chk_house_bind_status` CHECK (`bind_status` in (0,1))
) ENGINE = InnoDB AUTO_INCREMENT = 93 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房屋表（业主绑定的房屋）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
                               `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告内容',
                               `target_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ALL' COMMENT '目标类型: ALL/COMMUNITY/BUILDING/USER',
                               `target_user_id` bigint NULL DEFAULT NULL COMMENT '指定用户ID(为空则全员可见)',
                               `community_id` bigint NULL DEFAULT NULL COMMENT '指定小区ID',
                               `community_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '小区名称',
                               `building_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '指定楼栋号',
                               `publish_status` enum('DRAFT','PUBLISHED','OFFLINE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发布状态（DRAFT/PUBLISHED/OFFLINE）',
                               `top_flag` tinyint(1) NULL DEFAULT NULL COMMENT '是否置顶（true/false）',
                               `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
                               `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
                               `creator_id` bigint NULL DEFAULT NULL COMMENT '创建人ID（管理员ID）',
                               `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除（逻辑删除）',
                               `target_building` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '定向楼栋号',
                               PRIMARY KEY (`id`) USING BTREE,
                               INDEX `idx_community_id`(`community_id` ASC) USING BTREE,
                               INDEX `idx_publish_status`(`publish_status` ASC) USING BTREE,
                               INDEX `idx_target_user`(`target_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 83 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_notice_read
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice_read`;
CREATE TABLE `sys_notice_read`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `notice_id` bigint NOT NULL COMMENT '关联的公告ID',
                                    `user_id` bigint NOT NULL COMMENT '用户ID（业主ID）',
                                    `status` enum('UNREAD','READ') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'UNREAD' COMMENT '阅读状态（UNREAD/READ）',
                                    `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
                                    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    UNIQUE INDEX `uk_notice_user`(`notice_id` ASC, `user_id` ASC) USING BTREE COMMENT '公告+用户唯一索引（避免重复记录）',
                                    INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                                    INDEX `idx_status`(`status` ASC) USING BTREE,
                                    CONSTRAINT `fk_notice_read_notice` FOREIGN KEY (`notice_id`) REFERENCES `sys_notice` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公告阅读记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
                                 `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '模块标题',
                                 `business_type` int NULL DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
                                 `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '方法名称',
                                 `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求方式',
                                 `operator_type` int NULL DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
                                 `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作人员',
                                 `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求URL',
                                 `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '主机地址',
                                 `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作地点',
                                 `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求参数',
                                 `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '返回参数',
                                 `status` int NULL DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
                                 `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '错误消息',
                                 `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 105 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID（自增）',
                             `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录用户名（不可重复，如业主1、admin）',
                             `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码（MVP阶段暂存明文，后续需用BCrypt加密）',
                             `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '真实姓名（如张三）',
                             `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '手机号（可选，用于通知）',
                             `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色：OWNER(业主), ADMIN(物业管理员), SUPER_ADMIN(系统超管)',
                             `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态: 1正常, 0禁用',
                             `balance` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '账户余额',
                             `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             `community_id` bigint NULL DEFAULT NULL COMMENT '所属社区ID（管理员用）',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `username`(`username` ASC) USING BTREE,
                             UNIQUE INDEX `phone`(`phone` ASC) USING BTREE,
                             INDEX `idx_role`(`role` ASC) USING BTREE COMMENT '按角色查询索引（管理员查业主时用）',
                             INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 51 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表（业主+管理员）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_visitor
-- ----------------------------
DROP TABLE IF EXISTS `sys_visitor`;
CREATE TABLE `sys_visitor`  (
                                `id` bigint NOT NULL AUTO_INCREMENT,
                                `user_id` bigint NOT NULL,
                                `community_id` bigint NULL DEFAULT NULL,
                                `visitor_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                `visitor_phone` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                `visit_time` datetime NULL DEFAULT NULL,
                                `car_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                                `create_time` datetime NULL DEFAULT NULL,
                                `update_time` datetime NULL DEFAULT NULL,
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '访客预约' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_house
-- ----------------------------
DROP TABLE IF EXISTS `user_house`;
CREATE TABLE `user_house`  (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `user_id` bigint NOT NULL,
                               `house_id` bigint NOT NULL,
                               `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
                               `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '待审核',
                               `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               PRIMARY KEY (`id`) USING BTREE,
                               UNIQUE INDEX `uk_user_house`(`user_id` ASC, `house_id` ASC) USING BTREE,
                               INDEX `house_id`(`house_id` ASC) USING BTREE,
                               CONSTRAINT `user_house_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                               CONSTRAINT `user_house_ibfk_2` FOREIGN KEY (`house_id`) REFERENCES `sys_house` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 66 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
