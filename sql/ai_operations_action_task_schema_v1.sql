SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_operations_action_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI运营督办任务ID',
  `task_batch_no` varchar(64) NOT NULL COMMENT '同一次生成的任务批次号',
  `community_id` bigint NULL DEFAULT NULL COMMENT '社区ID',
  `community_name` varchar(100) NULL DEFAULT NULL COMMENT '社区名称',
  `source` varchar(20) NOT NULL DEFAULT 'AI_INSIGHTS' COMMENT '来源：AI_INSIGHTS/MANUAL',
  `source_version` varchar(64) NULL DEFAULT NULL COMMENT '来源版本，如 operations-ai-v1',
  `source_window_start` date NULL DEFAULT NULL COMMENT '统计开始日期',
  `source_window_end` date NULL DEFAULT NULL COMMENT '统计结束日期',
  `overall_risk_level` varchar(20) NULL DEFAULT NULL COMMENT '洞察整体风险等级',
  `priority` varchar(10) NOT NULL COMMENT 'P0/P1/P2/P3',
  `owner_role` varchar(100) NOT NULL COMMENT '责任角色',
  `task_title` varchar(255) NOT NULL COMMENT '任务标题',
  `task_reason` varchar(1000) NULL DEFAULT NULL COMMENT '任务原因',
  `deadline_text` varchar(100) NULL DEFAULT NULL COMMENT '原始截止描述，如24小时内',
  `status` varchar(20) NOT NULL DEFAULT 'TODO' COMMENT 'TODO/IN_PROGRESS/BLOCKED/DONE/CANCELLED',
  `feedback_result` varchar(1000) NULL DEFAULT NULL COMMENT '执行反馈结果',
  `feedback_by` varchar(100) NULL DEFAULT NULL COMMENT '反馈人',
  `feedback_time` datetime NULL DEFAULT NULL COMMENT '反馈时间',
  `closed_loop` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已闭环',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_task_batch_no` (`task_batch_no`) USING BTREE,
  KEY `idx_community_status` (`community_id`, `status`) USING BTREE,
  KEY `idx_priority_status` (`priority`, `status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'AI运营洞察督办任务表'
  ROW_FORMAT = DYNAMIC;
