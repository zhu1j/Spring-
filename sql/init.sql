-- ============================================================
-- 跨境电商客户资料管理系统 - 数据库初始化脚本
-- 版本: 1.0.0
-- 日期: 2026-06-30
-- 说明: 创建数据库及核心表结构，包含初始化数据
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS crm_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE crm_db;

-- ============================================================
-- 1. 客户信息主表
-- ============================================================
DROP TABLE IF EXISTS customer;
CREATE TABLE customer (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY
    COMMENT '主键ID',

    -- 基本信息
    company_name    VARCHAR(200)    NOT NULL
    COMMENT '公司/店铺名称',
    contact_person  VARCHAR(100)    NOT NULL
    COMMENT '联系人姓名',
    phone           VARCHAR(30)     DEFAULT NULL
    COMMENT '联系电话',
    email           VARCHAR(100)    DEFAULT NULL
    COMMENT '邮箱地址',
    country         VARCHAR(50)     DEFAULT NULL
    COMMENT '国家/地区',
    address         VARCHAR(500)    DEFAULT NULL
    COMMENT '详细地址',
    website         VARCHAR(200)    DEFAULT NULL
    COMMENT '公司/店铺网址',
    wechat          VARCHAR(50)     DEFAULT NULL
    COMMENT '微信号',

    -- 业务分类
    customer_type   VARCHAR(50)     NOT NULL
    COMMENT '客户类型: 店铺购买, 开店培训, 合作工厂, 物流合作, 其他',
    service_needs   TEXT            DEFAULT NULL
    COMMENT '服务需求描述',

    -- 客户来源 & 状态
    source          VARCHAR(100)    DEFAULT NULL
    COMMENT '客户来源渠道',
    status          VARCHAR(20)     DEFAULT '潜在客户'
    COMMENT '跟进状态: 潜在客户, 初步接洽, 意向明确, 已签约, 已流失',

    -- 跟进信息
    remark          TEXT            DEFAULT NULL
    COMMENT '备注信息',
    created_by      VARCHAR(100)    DEFAULT NULL
    COMMENT '录入人',

    -- 审计字段
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP
    COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    COMMENT '最后更新时间',
    is_deleted      TINYINT(1)      DEFAULT 0
    COMMENT '逻辑删除标记: 0=正常, 1=已删除',

    INDEX idx_company_name (company_name),
    INDEX idx_customer_type (customer_type),
    INDEX idx_status (status),
    INDEX idx_country (country),
    INDEX idx_created_at (created_at)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    COMMENT = '客户信息主表';

-- ============================================================
-- 2. 初始化示例数据
-- ============================================================
INSERT INTO customer (company_name, contact_person, phone, email, country, address, website, wechat, customer_type, service_needs, source, status, remark, created_by) VALUES
('深圳跨境贸易有限公司', '张三', '13800138001', 'zhangsan@example.com', '中国', '广东省深圳市南山区科技园路1号', 'https://szkt.example.com', 'zhangsan_wx', '店铺购买', '需要购买东南亚市场的店铺，月销目标$5000+', '官网咨询', '已签约', '已签署购买协议，店铺已交付', '李经理'),
('Amazon大卖工作室', 'John Smith', '+1-555-0101', 'john@amzstudio.com', '美国', '123 Main St, Los Angeles, CA 90001', 'https://amzstudio.com', NULL, '开店培训', '团队5人需要系统化运营培训，包括广告投放和Listing优化', '展会获取', '意向明确', '已发送培训方案，等待确认', '李经理'),
('东莞优品制造厂', '王厂长', '13900139002', 'wang@youpin.cn', '中国', '广东省东莞市长安镇工业大道88号', NULL, 'wang_wx888', '合作工厂', '可提供小家电品类供货，月产能2万件，寻求联合运营', '朋友推荐', '意向明确', '已寄样品，质检通过', '赵主管'),
('Global Logistics Inc.', 'Sarah Lee', '+82-2-555-0303', 'sarah@globallog.com', '韩国', '456 Gangnam-daero, Seoul', 'https://globallog.com', 'sarah_global', '物流合作', '提供韩国海外仓服务，寻求长期合作', 'LinkedIn', '初步接洽', '已视频会议沟通，下一步谈价格', '赵主管'),
('上海启航电子商务有限公司', '陈小明', '13700137003', 'chenxm@qihang.cn', '中国', '上海市浦东新区张江高科技园区', 'https://qihang.shop', 'cxm_2026', '店铺购买', '需要欧洲站店铺，要求有品牌备案', '百度广告', '潜在客户', '已电话沟通，约下周面谈', '王助理'),
('Tokyo Trend Traders', 'Tanaka Hiroshi', '+81-3-5555-0404', 'tanaka@ttt.co.jp', '日本', '1-2-3 Shibuya, Tokyo', 'https://ttt.co.jp', 'tanaka_h', '开店培训', '日本本土卖家转型跨境，需要全套开店指导', '行业峰会', '已签约', '培训进行中，第一期已完成', '王助理'),
('宁波跨境供应链有限公司', '刘经理', '13500135004', 'liu@nbscm.cn', '中国', '浙江省宁波市北仑区保税区', NULL, NULL, '合作工厂', '提供家居用品全品类供货，有海外仓备货能力', '1688平台', '初步接洽', '资料已收，等待内部评审', '赵主管'),
('EU Market Experts', 'Maria Müller', '+49-30-5555-0505', 'maria@eumarket.de', '德国', 'Unter den Linden 5, Berlin', 'https://eumarket.de', NULL, '开店培训', '帮助其中国供应商客户开设平台店铺', '邮件营销', '潜在客户', '是新线索，需安排首次沟通', '王助理');

-- ============================================================
-- 3. 操作日志表（可选扩展）
-- ============================================================
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY
    COMMENT '主键ID',
    customer_id     BIGINT          NOT NULL
    COMMENT '关联客户ID',
    operation_type  VARCHAR(50)     NOT NULL
    COMMENT '操作类型: 创建, 编辑, 导出, 状态变更',
    content         TEXT            DEFAULT NULL
    COMMENT '操作描述',
    operator        VARCHAR(100)    DEFAULT NULL
    COMMENT '操作人',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP
    COMMENT '操作时间',

    INDEX idx_customer_id (customer_id),
    INDEX idx_created_at (created_at)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    COMMENT = '操作日志表';

-- 验证数据
SELECT COUNT(*) AS customer_count FROM customer;
SELECT * FROM customer LIMIT 3;
