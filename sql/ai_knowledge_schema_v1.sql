SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- AI knowledge document table
-- ------------------------------------------------------------
-- This table stores the original knowledge items used by the RAG assistant.
-- In v1 it supports MySQL keyword retrieval. Later it can be synchronized to
-- a vector database or an embedding-backed vector_store table.
CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识文档ID',
  `source_type` varchar(32) NOT NULL COMMENT '知识来源类型：COMMUNITY_NOTICE/PROPERTY_POLICY/REPAIR_PROCESS/PROPERTY_PROCESS/FAQ',
  `source_id` varchar(64) NOT NULL COMMENT '业务侧唯一来源ID，例如 NOTICE_WATER_001 或 sys_notice:83',
  `community_id` bigint NULL DEFAULT NULL COMMENT '所属社区ID；NULL表示所有社区通用',
  `title` varchar(200) NOT NULL COMMENT '知识标题',
  `content` mediumtext NOT NULL COMMENT '知识正文',
  `keywords` varchar(1000) NULL DEFAULT NULL COMMENT '关键词，逗号分隔，用于v1关键词检索',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED/DRAFT',
  `visibility` varchar(20) NOT NULL DEFAULT 'RESIDENT' COMMENT '可见范围：RESIDENT/STAFF/ALL',
  `effective_time` datetime NULL DEFAULT NULL COMMENT '生效时间',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `origin_table` varchar(64) NULL DEFAULT NULL COMMENT '原始业务表名，例如 sys_notice',
  `origin_id` bigint NULL DEFAULT NULL COMMENT '原始业务表主键',
  `content_hash` varchar(64) NULL DEFAULT NULL COMMENT '正文hash，用于判断是否需要重新切块/同步',
  `version` int NOT NULL DEFAULT 1 COMMENT '知识版本号',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_source` (`source_type`, `source_id`) USING BTREE,
  KEY `idx_community_status` (`community_id`, `status`, `deleted`) USING BTREE,
  KEY `idx_origin` (`origin_table`, `origin_id`) USING BTREE,
  KEY `idx_effective_expire` (`effective_time`, `expire_time`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'AI RAG知识文档表'
  ROW_FORMAT = DYNAMIC;

-- ------------------------------------------------------------
-- AI knowledge chunk table
-- ------------------------------------------------------------
-- This table stores retrievable chunks. v1 can search title/content/keywords
-- with LIKE or application-side keyword scoring. Later each chunk can be
-- embedded and synchronized to a vector store.
CREATE TABLE IF NOT EXISTS `ai_knowledge_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识分块ID',
  `document_id` bigint NOT NULL COMMENT '关联ai_knowledge_document.id',
  `chunk_no` int NOT NULL COMMENT '文档内分块序号，从1开始',
  `chunk_title` varchar(200) NULL DEFAULT NULL COMMENT '分块标题',
  `chunk_content` text NOT NULL COMMENT '分块正文',
  `keywords` varchar(1000) NULL DEFAULT NULL COMMENT '分块关键词，逗号分隔',
  `char_count` int NULL DEFAULT NULL COMMENT '分块字符数',
  `content_hash` varchar(64) NULL DEFAULT NULL COMMENT '分块正文hash',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_document_chunk` (`document_id`, `chunk_no`) USING BTREE,
  KEY `idx_document_id` (`document_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  CONSTRAINT `fk_ai_knowledge_chunk_document`
    FOREIGN KEY (`document_id`) REFERENCES `ai_knowledge_document` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'AI RAG知识分块表'
  ROW_FORMAT = DYNAMIC;

-- ------------------------------------------------------------
-- AI knowledge sync log table
-- ------------------------------------------------------------
-- This table records sync/import/rebuild operations. It helps explain where a
-- RAG document came from and why a chunk was refreshed.
CREATE TABLE IF NOT EXISTS `ai_knowledge_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '同步日志ID',
  `sync_batch_no` varchar(64) NULL DEFAULT NULL COMMENT '同步批次号',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_id` varchar(64) NOT NULL COMMENT '来源ID',
  `document_id` bigint NULL DEFAULT NULL COMMENT '关联ai_knowledge_document.id',
  `sync_action` varchar(32) NOT NULL COMMENT '动作：CREATE/UPDATE/DISABLE/DELETE/REBUILD_CHUNK',
  `sync_status` varchar(20) NOT NULL COMMENT '状态：SUCCESS/FAILED/SKIPPED',
  `message` varchar(1000) NULL DEFAULT NULL COMMENT '同步说明或失败原因',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_batch_no` (`sync_batch_no`) USING BTREE,
  KEY `idx_source` (`source_type`, `source_id`) USING BTREE,
  KEY `idx_document_id` (`document_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'AI RAG知识同步日志表'
  ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- Initial RAG demo knowledge
-- ------------------------------------------------------------
INSERT INTO `ai_knowledge_document`
(`source_type`, `source_id`, `community_id`, `title`, `content`, `keywords`, `status`, `visibility`, `origin_table`, `origin_id`, `version`)
VALUES
('REPAIR_PROCESS', 'PROCESS_REPAIR_001', NULL, '居民报修处理流程',
 '居民可在小程序或业主端提交报修，填写故障类型、故障描述、房屋信息和图片。物业受理后生成工单，维修人员接单并上门处理。紧急漏水、漏电、电梯困人、燃气异味等情况应优先电话联系物业值班人员。',
 '报修,维修,工单,漏水,漏电,上门,水管,电路,电梯困人', 'ENABLED', 'RESIDENT', NULL, NULL, 1),
('PROPERTY_POLICY', 'POLICY_REPAIR_001', NULL, '维修响应时效制度',
 '普通报修建议在24小时内响应；较急问题建议在2小时内响应；漏水、漏电、电梯故障、消防隐患等紧急问题建议30分钟内响应；涉及人身安全的高危事件应立即人工确认并联系应急人员。',
 '响应,时效,24小时,2小时,30分钟,紧急,漏水,漏电,消防,高危', 'ENABLED', 'RESIDENT', NULL, NULL, 1),
('COMMUNITY_NOTICE', 'NOTICE_WATER_001', 1, '二次供水水箱清洗停水通知',
 '因二次供水水箱清洗，3栋和4栋计划在本周六09:00至12:00暂停供水。请居民提前储水，停水期间关闭家中水龙头，恢复供水后可先短暂放水。',
 '停水,供水,水箱,3栋,4栋,周六,储水,水龙头', 'ENABLED', 'RESIDENT', 'sys_notice', NULL, 1),
('PROPERTY_POLICY', 'POLICY_FEE_001', NULL, '物业费缴费与催缴说明',
 '物业费可通过业主端线上缴纳，也可到物业服务中心线下缴纳。若产生欠费，系统会发送缴费提醒。对费用明细有疑问时，居民可联系物业前台核对房屋、周期、金额和缴费记录。',
 '物业费,缴费,欠费,催缴,费用,发票,明细,线上缴纳', 'ENABLED', 'RESIDENT', NULL, NULL, 1),
('PROPERTY_PROCESS', 'PROCESS_COMPLAINT_001', NULL, '居民投诉处理流程',
 '居民可提交投诉建议，物业客服登记后分派给相关岗位处理。一般投诉应在1个工作日内受理，处理完成后记录处理结果。涉及安全、秩序、噪音等问题应保留现场信息和时间描述。',
 '投诉,建议,噪音,秩序,安全,处理结果,客服,受理', 'ENABLED', 'RESIDENT', NULL, NULL, 1),
('PROPERTY_POLICY', 'POLICY_VISITOR_001', NULL, '访客登记制度',
 '访客进入小区前应进行访客登记，填写来访人、被访人、手机号和来访时间。门岗核验后放行。长期施工、装修人员应按物业要求办理临时出入手续。',
 '访客,登记,门岗,来访,装修,施工,出入', 'ENABLED', 'RESIDENT', NULL, NULL, 1)
ON DUPLICATE KEY UPDATE
  `community_id` = VALUES(`community_id`),
  `title` = VALUES(`title`),
  `content` = VALUES(`content`),
  `keywords` = VALUES(`keywords`),
  `status` = VALUES(`status`),
  `visibility` = VALUES(`visibility`),
  `origin_table` = VALUES(`origin_table`),
  `origin_id` = VALUES(`origin_id`),
  `version` = VALUES(`version`),
  `update_time` = CURRENT_TIMESTAMP;

INSERT INTO `ai_knowledge_chunk`
(`document_id`, `chunk_no`, `chunk_title`, `chunk_content`, `keywords`, `char_count`, `status`)
SELECT `id`, 1, `title`, `content`, `keywords`, CHAR_LENGTH(`content`), 'ENABLED'
FROM `ai_knowledge_document`
WHERE (`source_type`, `source_id`) IN (
  ('REPAIR_PROCESS', 'PROCESS_REPAIR_001'),
  ('PROPERTY_POLICY', 'POLICY_REPAIR_001'),
  ('COMMUNITY_NOTICE', 'NOTICE_WATER_001'),
  ('PROPERTY_POLICY', 'POLICY_FEE_001'),
  ('PROPERTY_PROCESS', 'PROCESS_COMPLAINT_001'),
  ('PROPERTY_POLICY', 'POLICY_VISITOR_001')
)
ON DUPLICATE KEY UPDATE
  `chunk_title` = VALUES(`chunk_title`),
  `chunk_content` = VALUES(`chunk_content`),
  `keywords` = VALUES(`keywords`),
  `char_count` = VALUES(`char_count`),
  `status` = VALUES(`status`),
  `update_time` = CURRENT_TIMESTAMP;
