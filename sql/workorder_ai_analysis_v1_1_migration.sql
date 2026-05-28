ALTER TABLE `biz_work_order_ai_analysis`
  ADD COLUMN `urgency_level` varchar(32) DEFAULT NULL COMMENT 'AI urgency level: LOW/MEDIUM/HIGH/CRITICAL' AFTER `priority`,
  ADD COLUMN `extracted_location` varchar(255) DEFAULT NULL COMMENT 'AI extracted location supplement from repair text' AFTER `summary`,
  ADD COLUMN `suggested_response_minutes` int DEFAULT NULL COMMENT 'AI suggested response time limit in minutes' AFTER `extracted_location`,
  ADD COLUMN `safety_tips` text COMMENT 'AI safety tips as JSON array text' AFTER `suggested_response_minutes`;
