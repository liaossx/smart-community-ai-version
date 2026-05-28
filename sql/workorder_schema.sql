-- 创建工单表
CREATE TABLE IF NOT EXISTS `biz_work_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `repair_id` bigint(20) NOT NULL COMMENT '关联报修单ID',
  `order_no` varchar(32) NOT NULL COMMENT '工单编号',
  `community_id` bigint(20) DEFAULT NULL COMMENT '所属社区ID',
  `worker_id` bigint(20) DEFAULT NULL COMMENT '维修员ID',
  `worker_name` varchar(64) DEFAULT NULL COMMENT '维修员姓名',
  `worker_phone` varchar(20) DEFAULT NULL COMMENT '维修员电话',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待指派, ASSIGNED-已指派, PROCESSING-处理中, COMPLETED-已完成, CANCELLED-已取消',
  `priority` tinyint(4) DEFAULT '1' COMMENT '优先级: 1-普通, 2-紧急, 3-特急',
  `plan_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
  `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
  `process_result` text COMMENT '处理结果',
  `process_imgs` text COMMENT '处理后图片',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_repair_id` (`repair_id`),
  KEY `idx_worker_id` (`worker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 模拟一些维修员数据 (在 sys_user 中)
-- 注意：这里只是注释，实际需要在 user-service 的数据库中执行
-- INSERT INTO sys_user (username, password, real_name, phone, role, community_id) VALUES 
-- ('worker1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张师傅', '13511112222', 'worker', 1),
-- ('worker2', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李师傅', '13533334444', 'worker', 1);
