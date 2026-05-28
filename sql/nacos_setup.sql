/*
 * 本 SQL 脚本适用于 Nacos 2.x (包括 2.2.0)
 * 请在目标数据库中执行此脚本
 */

-- 1. 创建数据库 (如果尚未创建)
CREATE DATABASE IF NOT EXISTS nacos_config
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE nacos_config;

-- 2. 创建配置信息表 (核心表)
DROP TABLE IF EXISTS config_info;
CREATE TABLE config_info (
                             id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                             data_id varchar(255) NOT NULL COMMENT 'data_id',
                             group_id varchar(255) DEFAULT NULL,
                             content longtext NOT NULL COMMENT 'content',
                             md5 varchar(32) DEFAULT NULL COMMENT 'md5',
                             gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                             src_user text COMMENT 'source user',
                             src_ip varchar(50) DEFAULT NULL COMMENT 'source ip',
                             app_name varchar(128) DEFAULT NULL,
                             tenant_id varchar(128) DEFAULT '' COMMENT '租户字段',
                             c_desc varchar(256) DEFAULT NULL,
                             c_use varchar(64) DEFAULT NULL,
                             effect varchar(64) DEFAULT NULL,
                             type varchar(64) DEFAULT NULL,
                             c_schema text,
                             encrypted_data_key text NOT NULL COMMENT '密钥',
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_configinfo_datagrouptenant (data_id,group_id,tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='config_info';

-- 3. 创建配置聚合表
DROP TABLE IF EXISTS config_info_aggr;
CREATE TABLE config_info_aggr (
                                  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                                  data_id varchar(255) NOT NULL COMMENT 'data_id',
                                  group_id varchar(255) NOT NULL COMMENT 'group_id',
                                  datum_id varchar(255) NOT NULL COMMENT 'datum_id',
                                  content longtext NOT NULL COMMENT '内容',
                                  gmt_modified datetime NOT NULL COMMENT '修改时间',
                                  app_name varchar(128) DEFAULT NULL,
                                  tenant_id varchar(128) DEFAULT '' COMMENT '租户字段',
                                  PRIMARY KEY (id),
                                  UNIQUE KEY uk_configinfoaggr_datagrouptenantdatum (data_id,group_id,tenant_id,datum_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='增加租户字段';

-- 4. 创建配置变更历史表
DROP TABLE IF EXISTS config_info_beta;
CREATE TABLE config_info_beta (
                                  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                                  data_id varchar(255) NOT NULL COMMENT 'data_id',
                                  group_id varchar(128) NOT NULL COMMENT 'group_id',
                                  app_name varchar(128) DEFAULT NULL COMMENT 'app_name',
                                  content longtext NOT NULL COMMENT 'content',
                                  beta_ips varchar(1024) DEFAULT NULL COMMENT 'betaIps',
                                  md5 varchar(32) DEFAULT NULL COMMENT 'md5',
                                  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                                  src_user text COMMENT 'source user',
                                  src_ip varchar(50) DEFAULT NULL COMMENT 'source ip',
                                  tenant_id varchar(128) DEFAULT '' COMMENT '租户字段',
                                  encrypted_data_key text NOT NULL COMMENT '密钥',
                                  PRIMARY KEY (id),
                                  UNIQUE KEY uk_configinfobeta_datagrouptenant (data_id,group_id,tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='config_info';

-- 5. 创建配置标签表
DROP TABLE IF EXISTS config_info_tag;
CREATE TABLE config_info_tag (
                                 id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                                 data_id varchar(255) NOT NULL COMMENT 'data_id',
                                 group_id varchar(128) NOT NULL COMMENT 'group_id',
                                 tenant_id varchar(128) DEFAULT '' COMMENT 'tenant_id',
                                 tag_id varchar(128) NOT NULL COMMENT 'tag_id',
                                 tag_type varchar(64) DEFAULT NULL COMMENT 'tag_type',
                                 data_id_md5 varchar(32) DEFAULT NULL COMMENT 'data_id_md5',
                                 group_id_md5 varchar(32) DEFAULT NULL COMMENT 'group_id_md5',
                                 content longtext NOT NULL COMMENT 'content',
                                 md5 varchar(32) DEFAULT NULL COMMENT 'md5',
                                 gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                                 src_user text COMMENT 'source user',
                                 src_ip varchar(50) DEFAULT NULL COMMENT 'source ip',
                                 app_name varchar(128) DEFAULT NULL,
                                 encrypted_data_key text NOT NULL COMMENT '密钥',
                                 PRIMARY KEY (id),
                                 UNIQUE KEY uk_configinfotag_datagrouptenanttag (data_id,group_id,tenant_id,tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='config_info';

-- 6. 创建配置历史表
DROP TABLE IF EXISTS his_config_info;
CREATE TABLE his_config_info (
                                 id bigint(64) unsigned NOT NULL,
                                 nid bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                 data_id varchar(255) NOT NULL,
                                 group_id varchar(128) NOT NULL,
                                 app_name varchar(128) DEFAULT NULL COMMENT 'app_name',
                                 content longtext NOT NULL,
                                 md5 varchar(32) DEFAULT NULL,
                                 gmt_create datetime NOT NULL DEFAULT '2010-05-05 00:00:00',
                                 gmt_modified datetime NOT NULL DEFAULT '2010-05-05 00:00:00',
                                 src_user text,
                                 src_ip varchar(50) DEFAULT NULL,
                                 op_type char(10) DEFAULT NULL,
                                 tenant_id varchar(128) DEFAULT '' COMMENT '租户字段',
                                 encrypted_data_key text NOT NULL COMMENT '密钥',
                                 PRIMARY KEY (nid),
                                 KEY idx_gmt_create (gmt_create),
                                 KEY idx_gmt_modified (gmt_modified),
                                 KEY idx_did (data_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='配置历史表';

-- 7. 创建租户容量表
DROP TABLE IF EXISTS tenant_capacity;
CREATE TABLE tenant_capacity (
                                 id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 tenant_id varchar(128) NOT NULL DEFAULT '' COMMENT 'Tenant ID',
                                 quota bigint(20) NOT NULL DEFAULT '0' COMMENT '配额',
                                 usage bigint(20) NOT NULL DEFAULT '0' COMMENT '使用量',
                                 max_size bigint(20) NOT NULL DEFAULT '0' COMMENT '单个配置大小上限',
                                 max_aggr_count bigint(20) NOT NULL DEFAULT '0' COMMENT '聚合子配置最大数目',
                                 max_aggr_size bigint(20) NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限',
                                 max_history_count bigint(20) NOT NULL DEFAULT '0' COMMENT '变更历史最大数目',
                                 gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                                 PRIMARY KEY (id),
                                 UNIQUE KEY uk_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='租户容量';

-- 8. 创建租户信息表
DROP TABLE IF EXISTS tenant_info;
CREATE TABLE tenant_info (
                             id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                             kp varchar(128) NOT NULL COMMENT 'kp',
                             tenant_id varchar(128) DEFAULT '' COMMENT 'tenant_id',
                             tenant_name varchar(128) DEFAULT '' COMMENT 'tenant_name',
                             tenant_desc varchar(256) DEFAULT NULL COMMENT 'tenant_desc',
                             create_source varchar(32) DEFAULT NULL COMMENT 'create_source',
                             gmt_create bigint(20) NOT NULL COMMENT '创建时间',
                             gmt_modified bigint(20) NOT NULL COMMENT '修改时间',
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_tenant_info_kptenantid (kp,tenant_id),
                             KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='tenant_info';

-- 9. 创建用户表
DROP TABLE IF EXISTS users;
CREATE TABLE users (
                       username varchar(50) NOT NULL PRIMARY KEY,
                       password varchar(500) NOT NULL,
                       enabled boolean NOT NULL
);

-- 10. 创建角色表
DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
                       username varchar(50) NOT NULL,
                       role varchar(50) NOT NULL,
                       UNIQUE INDEX idx_user_role (username ASC, role ASC) USING BTREE
);

-- 11. 创建权限表
DROP TABLE IF EXISTS permissions;
CREATE TABLE permissions (
                             role varchar(50) NOT NULL,
                             resource varchar(255) NOT NULL,
                             action varchar(8) NOT NULL,
                             UNIQUE INDEX uk_permission_role_resource_permission (role, resource, action) USING BTREE
);

-- 12. 插入默认数据 (用户名/密码均为 nacos)
-- 注意：Nacos 2.x 默认密码是 nacos，这里使用加密后的密码字符串
INSERT INTO users (username, password, enabled) VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', TRUE);
INSERT INTO roles (username, role) VALUES ('nacos', 'ROLE_ADMIN');

COMMIT;