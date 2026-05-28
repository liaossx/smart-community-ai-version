-- 车位信息表
CREATE TABLE IF NOT EXISTS biz_parking_space (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    community_id        BIGINT          NULL,
    community_name      VARCHAR(128)    NULL,
    space_no            VARCHAR(64)     NOT NULL COMMENT '车位编号',
    space_type          VARCHAR(32)     NOT NULL COMMENT '车位类型：TEMP（临时）/ FIXED（固定）',
    status              VARCHAR(32)     NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE（可用）/ OCCUPIED（已占用）/ RESERVED（已预订）',
    bind_user_id        BIGINT          NULL COMMENT '绑定用户ID',
    bind_house_id       BIGINT          NULL COMMENT '绑定房屋ID',
    bind_time           DATETIME        NULL COMMENT '绑定时间',
    lease_end_time      DATETIME        NULL COMMENT '租赁结束时间',
    deleted             TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否删除：0未删除，1已删除',
    create_time         DATETIME        NOT NULL COMMENT '创建时间',
    update_time         DATETIME        NOT NULL COMMENT '更新时间',
    INDEX idx_community_id (community_id),
    INDEX idx_space_no (space_no),
    INDEX idx_status (status),
    INDEX idx_bind_user_id (bind_user_id)
) COMMENT '车位信息表';

-- 车位授权表
CREATE TABLE IF NOT EXISTS biz_parking_authorize (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id            BIGINT          NOT NULL COMMENT '车位ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    authorized_name     VARCHAR(64)     NOT NULL COMMENT '授权人姓名',
    authorized_phone    VARCHAR(32)     NOT NULL COMMENT '授权人手机号',
    plate_no            VARCHAR(32)     NOT NULL COMMENT '车牌号',
    start_time          DATETIME        NOT NULL COMMENT '授权开始时间',
    end_time            DATETIME        NULL COMMENT '授权结束时间',
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE（激活）/ EXPIRED（已过期）/ REVOKED（已撤销）',
    create_time         DATETIME        NOT NULL COMMENT '创建时间',
    update_time         DATETIME        NOT NULL COMMENT '更新时间',
    INDEX idx_space_id (space_id),
    INDEX idx_user_id (user_id),
    INDEX idx_plate_no (plate_no),
    INDEX idx_status (status)
) COMMENT '车位授权表';

-- 停车订单表
CREATE TABLE IF NOT EXISTS biz_parking_order (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no            VARCHAR(64)     NOT NULL UNIQUE COMMENT '订单编号',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    space_id            BIGINT          NOT NULL COMMENT '车位ID',
    order_type          VARCHAR(32)     NOT NULL COMMENT '订单类型：TEMP（临时）/ FIXED（固定）',
    amount              DECIMAL(10,2)   NOT NULL COMMENT '订单金额',
    paid_amount         DECIMAL(10,2)   NULL COMMENT '实付金额',
    status              VARCHAR(32)     NOT NULL DEFAULT 'UNPAID' COMMENT '状态：UNPAID（未支付）/ PAYING（支付中）/ PAID（已支付）/ CANCELLED（已取消）',
    start_time          DATETIME        NOT NULL COMMENT '停车开始时间',
    end_time            DATETIME        NULL COMMENT '停车结束时间',
    pay_time            DATETIME        NULL COMMENT '支付时间',
    pay_channel         VARCHAR(32)     NULL COMMENT '支付渠道',
    trade_no            VARCHAR(128)    NULL COMMENT '支付流水号',
    pay_remark          VARCHAR(255)    NULL COMMENT '支付备注',
    create_time         DATETIME        NOT NULL COMMENT '创建时间',
    update_time         DATETIME        NOT NULL COMMENT '更新时间',
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_space_id (space_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    UNIQUE KEY uk_trade_no (trade_no)
) COMMENT '停车订单表';


