-- Nacos 需要独立数据库，表结构由 Nacos Server 首次启动时自动创建
CREATE DATABASE IF NOT EXISTS `nacos_config` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
