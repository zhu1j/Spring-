-- ============================================================
-- H2 内存数据库初始化脚本（测试环境）
-- H2 使用 MySQL 兼容模式，DDL 与 MySQL 基本一致
-- ============================================================

DROP TABLE IF EXISTS customer;

CREATE TABLE customer (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name  VARCHAR(200)  NOT NULL COMMENT '公司/店铺名称',
    contact_person VARCHAR(100) NOT NULL COMMENT '联系人',
    phone         VARCHAR(50)   DEFAULT NULL COMMENT '联系电话',
    email         VARCHAR(100)  DEFAULT NULL COMMENT '邮箱',
    country       VARCHAR(100)  DEFAULT NULL COMMENT '国家/地区',
    address       VARCHAR(500)  DEFAULT NULL COMMENT '详细地址',
    website       VARCHAR(500)  DEFAULT NULL COMMENT '网站',
    wechat        VARCHAR(100)  DEFAULT NULL COMMENT '微信号',
    customer_type VARCHAR(50)   NOT NULL COMMENT '客户类型',
    service_needs TEXT          DEFAULT NULL COMMENT '服务需求描述',
    source        VARCHAR(100)  DEFAULT NULL COMMENT '客户来源渠道',
    status        VARCHAR(50)   DEFAULT '潜在客户' COMMENT '跟进状态',
    remark        VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    created_by    VARCHAR(100)  DEFAULT NULL COMMENT '录入人',
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted    TINYINT       DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除'
);

-- 插入测试数据
INSERT INTO customer (company_name, contact_person, phone, email, country, customer_type, status, source, created_by)
VALUES
('深圳环球贸易有限公司', '张三', '13800138001', 'zhangsan@global-trade.cn', '中国', '合作工厂', '已签约', '展会', 'admin'),
('Shanghai E-Commerce Co.', 'Alice Wang', '13900139002', 'alice@sh-ecom.com', '中国', '店铺购买', '意向明确', '官网', 'admin'),
('Tokyo 株式会社', '田中太郎', '+81-3-1234-5678', 'tanaka@tokyo-trading.jp', '日本', '开店培训', '初步接洽', '客户推荐', 'admin'),
('Seoul Trading Corp', 'Kim Min-ji', '+82-2-9876-5432', 'minji@seoul-trade.kr', '韩国', '物流合作', '潜在客户', '社交媒体', 'admin'),
('Global Sourcing Inc.', 'John Smith', '+1-415-555-0198', 'john@globalsource.com', '美国', '其他', '已流失', '邮件营销', 'admin');
