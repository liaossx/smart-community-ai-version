CREATE TABLE IF NOT EXISTS sys_notice (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(128) NOT NULL,
    content         TEXT        NOT NULL,
    target_type     VARCHAR(32) NOT NULL DEFAULT 'ALL',
    community_id    BIGINT      NULL,
    community_name  VARCHAR(128),
    building_no     VARCHAR(64),
    publish_status  VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    top_flag        TINYINT(1)  NOT NULL DEFAULT 0,
    publish_time    DATETIME    NULL,
    expire_time     DATETIME    NULL,
    creator_id      BIGINT      NOT NULL,
    create_time     DATETIME    NOT NULL,
    update_time     DATETIME    NOT NULL,
    deleted         TINYINT(1)  NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_notice_read (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    notice_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    status      VARCHAR(32) NOT NULL DEFAULT 'READ',
    read_time   DATETIME    NULL,
    create_time DATETIME    NOT NULL,
    UNIQUE KEY uk_notice_user (notice_id, user_id)
);

