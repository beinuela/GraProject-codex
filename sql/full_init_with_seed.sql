CREATE DATABASE IF NOT EXISTS campus_material DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE campus_material;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS login_log;
DROP TABLE IF EXISTS ai_call_log;
DROP TABLE IF EXISTS ai_analysis_task;
DROP TABLE IF EXISTS system_config;
DROP TABLE IF EXISTS event_record;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS storage_location;
DROP TABLE IF EXISTS campus;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS warning_record;
DROP TABLE IF EXISTS delivery_task;
DROP TABLE IF EXISTS transfer_order_item;
DROP TABLE IF EXISTS transfer_order;
DROP TABLE IF EXISTS apply_order_item;
DROP TABLE IF EXISTS apply_order;
DROP TABLE IF EXISTS stock_out_item;
DROP TABLE IF EXISTS stock_out;
DROP TABLE IF EXISTS stock_in_item;
DROP TABLE IF EXISTS stock_in;
DROP TABLE IF EXISTS inventory_batch;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS warehouse;
DROP TABLE IF EXISTS material_info;
DROP TABLE IF EXISTS material_category;
DROP TABLE IF EXISTS auth_refresh_token;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_dept;

CREATE TABLE sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    dept_id BIGINT,
    role_id BIGINT,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_dept (dept_id),
    INDEX idx_user_role (role_id)
);

CREATE TABLE auth_refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_id VARCHAR(64) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expire_at DATETIME NOT NULL,
    revoked TINYINT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refresh_token_id (token_id),
    INDEX idx_refresh_user (user_id),
    INDEX idx_refresh_expire (expire_at)
);

CREATE TABLE material_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50),
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_material_category_name (category_name)
);

CREATE TABLE material_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(50) NOT NULL UNIQUE,
    material_name VARCHAR(100) NOT NULL,
    category_id BIGINT,
    spec VARCHAR(100),
    unit VARCHAR(20),
    safety_stock INT NOT NULL DEFAULT 0,
    shelf_life_days INT,
    supplier VARCHAR(255),
    unit_price DECIMAL(10,2),
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_material_category (category_id)
);

CREATE TABLE warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code VARCHAR(50),
    warehouse_name VARCHAR(100) NOT NULL,
    campus_id BIGINT,
    campus VARCHAR(100),
    address VARCHAR(255),
    manager VARCHAR(100),
    contact_phone VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_warehouse_name (warehouse_name)
);

CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    current_qty INT NOT NULL DEFAULT 0,
    locked_qty INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_inventory_material_warehouse (material_id, warehouse_id)
);

CREATE TABLE inventory_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    batch_no VARCHAR(100) NOT NULL,
    in_qty INT NOT NULL DEFAULT 0,
    remain_qty INT NOT NULL DEFAULT 0,
    production_date DATE,
    expire_date DATE,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_batch_material_warehouse (material_id, warehouse_id),
    INDEX idx_batch_expire (expire_date),
    INDEX idx_batch_outbound_pick (material_id, warehouse_id, expire_date, remain_qty)
);

CREATE TABLE stock_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    source_type VARCHAR(50),
    operator_id BIGINT,
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stock_in_warehouse (warehouse_id)
);

CREATE TABLE stock_in_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_in_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    batch_no VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    production_date DATE,
    expire_date DATE,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stock_in_item_order (stock_in_id),
    INDEX idx_stock_in_item_material (material_id)
);

CREATE TABLE apply_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    urgency_level INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    scenario VARCHAR(255),
    fast_track TINYINT NOT NULL DEFAULT 0,
    reserved_warehouse_id BIGINT,
    approver_id BIGINT,
    approve_remark VARCHAR(255),
    approve_time DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_apply_status (status),
    INDEX idx_apply_dept (dept_id)
);

CREATE TABLE apply_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    apply_order_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    apply_qty INT NOT NULL,
    actual_qty INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_apply_item_order (apply_order_id),
    INDEX idx_apply_item_material (material_id)
);

CREATE TABLE stock_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    apply_order_id BIGINT,
    warehouse_id BIGINT NOT NULL,
    operator_id BIGINT,
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stock_out_apply (apply_order_id)
);

CREATE TABLE stock_out_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_out_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stock_out_item_order (stock_out_id),
    INDEX idx_stock_out_item_material (material_id),
    INDEX idx_stock_out_item_created_material (created_at, material_id)
);

CREATE TABLE transfer_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_warehouse_id BIGINT NOT NULL,
    to_warehouse_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    applicant_id BIGINT,
    approver_id BIGINT,
    approve_remark VARCHAR(255),
    approve_time DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_transfer_status (status)
);

CREATE TABLE transfer_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_order_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_transfer_item_order (transfer_order_id)
);

CREATE TABLE warning_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warning_type VARCHAR(50) NOT NULL,
    material_id BIGINT,
    warehouse_id BIGINT,
    content VARCHAR(255) NOT NULL,
    handle_status VARCHAR(30) NOT NULL DEFAULT 'UNHANDLED',
    handler_id BIGINT,
    handle_remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_warning_status (handle_status),
    INDEX idx_warning_type (warning_type),
    INDEX idx_warning_type_material_warehouse_status (warning_type, material_id, warehouse_id, handle_status)
);

CREATE TABLE delivery_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    apply_order_id BIGINT,
    stock_out_id BIGINT,
    receiver_name VARCHAR(100) NOT NULL,
    receiver_phone VARCHAR(50),
    delivery_address VARCHAR(255) NOT NULL,
    dispatcher_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    remark VARCHAR(255),
    signed_at DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_delivery_status (status),
    INDEX idx_delivery_apply (apply_order_id),
    INDEX idx_delivery_stock_out (stock_out_id)
);

CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT,
    module VARCHAR(50) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    detail VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_log_operator (operator_id),
    INDEX idx_log_module (module),
    INDEX idx_log_created_at (created_at)
);

CREATE TABLE ai_analysis_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_type VARCHAR(50) NOT NULL,
    biz_id BIGINT NOT NULL,
    request_snapshot TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    result_source VARCHAR(30) NOT NULL DEFAULT 'RULE_FALLBACK',
    result_json TEXT,
    error_message VARCHAR(500),
    created_by BIGINT,
    started_at DATETIME,
    finished_at DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_task_biz (biz_type, biz_id),
    INDEX idx_ai_task_status (status),
    INDEX idx_ai_task_created (created_at)
);

CREATE TABLE ai_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_template_code VARCHAR(100) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    success_flag TINYINT NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_call_task (task_id),
    INDEX idx_ai_call_created (created_at)
);

-- ===================== 校区管理 =====================
CREATE TABLE campus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campus_name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    manager VARCHAR(100),
    contact_phone VARCHAR(50),
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ===================== 库位管理 =====================
CREATE TABLE storage_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    location_code VARCHAR(50) NOT NULL,
    location_name VARCHAR(100) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    capacity INT NOT NULL DEFAULT 0,
    used_qty INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_location_warehouse (warehouse_id)
);

-- ===================== 供应商管理 =====================
CREATE TABLE supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    contact_phone VARCHAR(50),
    email VARCHAR(100),
    address VARCHAR(255),
    supply_scope VARCHAR(500),
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ===================== 事件记录管理 =====================
CREATE TABLE event_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_title VARCHAR(200) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_level VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    campus_id BIGINT,
    location VARCHAR(255),
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    reporter_id BIGINT,
    handler_id BIGINT,
    handle_result TEXT,
    event_time DATETIME NOT NULL,
    close_time DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_event_status (status),
    INDEX idx_event_type (event_type),
    INDEX idx_event_level (event_level)
);

-- ===================== 系统配置 =====================
CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500) NOT NULL,
    config_name VARCHAR(200) NOT NULL,
    config_group VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_group (config_group)
);

-- ===================== 登录日志 =====================
CREATE TABLE login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    login_ip VARCHAR(100),
    login_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent VARCHAR(500),
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_login_log_user (user_id),
    INDEX idx_login_log_time (login_time)
);

-- ===================== 通知消息 =====================
CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(500) NOT NULL,
    msg_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    target_user_id BIGINT,
    is_read TINYINT NOT NULL DEFAULT 0,
    biz_type VARCHAR(50),
    biz_id BIGINT,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notification_user (target_user_id),
    INDEX idx_notification_read (is_read),
    INDEX idx_notification_user_created (target_user_id, created_at)
);

SET FOREIGN_KEY_CHECKS = 1;


-- ===================== 初始化测试数据 =====================
USE campus_material;
SET NAMES utf8mb4;

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '系统管理员', '全局管理'),
(2, 'WAREHOUSE_ADMIN', '仓库管理员', '仓库与库存管理'),
(3, 'DEPT_USER', '部门用户', '物资申请与签收'),
(4, 'APPROVER', '审批人员', '审核申请与调拨'),
(5, 'PURCHASER', '采购人员', '供应商与入库管理'),
(6, 'DISPATCHER', '调度人员', '配送派单与签收跟踪'),
(7, 'USER', '普通用户', '物资申领与签收')
ON DUPLICATE KEY UPDATE
role_code = VALUES(role_code),
role_name = VALUES(role_name),
description = VALUES(description),
deleted = 0;

INSERT INTO sys_dept (id, dept_name, parent_id) VALUES
(1, '学校本部', NULL),
(2, '后勤处', 1),
(3, '保卫处', 1),
(4, '医务室', 1),
(5, '计算机学院', 1)
ON DUPLICATE KEY UPDATE
dept_name = VALUES(dept_name),
parent_id = VALUES(parent_id),
deleted = 0;

-- 演示账号密码统一为 Abc@123456（BCrypt 加密存储）
INSERT INTO sys_user (id, username, password, real_name, dept_id, role_id, status) VALUES
(1, 'admin', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '系统管理员', 2, 1, 1),
(2, 'warehouse', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '仓库管理员', 2, 2, 1),
(3, 'dept', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '部门申请员', 5, 3, 1),
(4, 'approver', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '审批负责人', 2, 4, 1),
(5, 'purchaser', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '采购专员', 2, 5, 1),
(6, 'dispatcher', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '配送调度员', 2, 6, 1),
(7, 'user', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '普通申领人', 5, 7, 1)
ON DUPLICATE KEY UPDATE
username = VALUES(username),
password = VALUES(password),
real_name = VALUES(real_name),
dept_id = VALUES(dept_id),
role_id = VALUES(role_id),
status = VALUES(status),
deleted = 0;

INSERT INTO auth_refresh_token (id, user_id, token_id, token_hash, expire_at, revoked, created_at, updated_at) VALUES
(1, 1, 'refresh-admin-20260515', 'demo_refresh_hash_admin_20260515', '2026-06-15 23:59:59', 0, '2026-05-15 08:30:00', '2026-05-15 08:30:00'),
(2, 2, 'refresh-warehouse-20260515', 'demo_refresh_hash_warehouse_20260515', '2026-06-15 23:59:59', 0, '2026-05-15 08:35:00', '2026-05-15 08:35:00');

INSERT INTO material_category (id, category_name, category_code, remark) VALUES
(1, '防疫类', 'CAT-FY', '口罩、消毒液等'),
(2, '医疗急救类', 'CAT-YL', '急救包、药品'),
(3, '食品饮水类', 'CAT-SP', '饮用水、压缩饼干'),
(4, '照明通信类', 'CAT-ZM', '手电、对讲机'),
(5, '防汛防灾类', 'CAT-FX', '雨衣、沙袋');

INSERT INTO material_info (id, material_code, material_name, category_id, spec, unit, safety_stock, shelf_life_days, supplier, unit_price, remark) VALUES
(1, 'M001', '医用口罩', 1, '50只/盒', '盒', 200, 1095, '华安防护', 35.00, '常备防疫物资'),
(2, 'M002', '84消毒液', 1, '5L/桶', '桶', 60, 365, '洁安化工', 42.50, '重点区域消杀'),
(3, 'M003', '急救包', 2, '标准型', '套', 30, 730, '康护医疗', 98.00, '医务室与宿舍楼备用'),
(4, 'M004', '瓶装饮用水', 3, '550ml*24', '箱', 120, 540, '清泉食品', 30.00, '集中疏散保障'),
(5, 'M005', '强光手电', 4, '可充电', '个', 40, 1825, '明锐电子', 65.00, '停电与夜间巡查');

INSERT INTO warehouse (id, warehouse_code, warehouse_name, campus_id, campus, address, manager, contact_phone, status, remark) VALUES
(1, 'WH-KX-ZC', '科学校区总仓', 1, '科学校区', '后勤楼B1层', '张老师', '0371-86601201', 'NORMAL', '主校区应急物资总仓'),
(2, 'WH-DF-FC', '东风校区分仓', 2, '东风校区', '体育馆北侧', '李老师', '0371-63556701', 'NORMAL', '东风校区保障仓'),
(3, 'WH-YW', '医务室物资仓', 1, '科学校区', '医务室一层', '王医生', '0371-86601220', 'NORMAL', '医疗急救物资仓');

INSERT INTO inventory (id, material_id, warehouse_id, current_qty, locked_qty) VALUES
(1, 1, 1, 520, 0),
(2, 2, 1, 110, 0),
(3, 3, 3, 28, 0),
(4, 4, 2, 220, 0),
(5, 5, 1, 55, 0),
(6, 1, 2, 160, 0),
(7, 3, 1, 14, 0);

INSERT INTO inventory_batch (material_id, warehouse_id, batch_no, in_qty, remain_qty, production_date, expire_date, created_at, updated_at) VALUES
(1, 1, 'MASK-2025A', 300, 220, '2025-10-01', '2028-10-01', '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(1, 1, 'MASK-2026A', 300, 300, '2026-01-15', '2029-01-15', '2026-02-10 10:00:00', '2026-02-10 10:00:00'),
(2, 1, 'DIS-2025A', 120, 110, '2025-12-01', '2026-11-30', '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(3, 3, 'MED-2024A', 20, 6, '2024-08-01', '2026-04-01', '2025-11-10 09:00:00', '2025-11-10 09:00:00'),
(3, 3, 'MED-2025A', 30, 22, '2025-08-01', '2027-08-01', '2026-01-12 09:00:00', '2026-01-12 09:00:00'),
(4, 2, 'WATER-2025A', 240, 220, '2025-12-20', '2027-06-12', '2026-01-18 10:00:00', '2026-01-18 10:00:00'),
(5, 1, 'LIGHT-2025A', 60, 55, '2025-06-01', '2030-05-31', '2026-01-05 10:00:00', '2026-01-05 10:00:00'),
(1, 2, 'MASK-2025B', 180, 160, '2025-09-15', '2028-09-15', '2026-01-20 09:00:00', '2026-01-20 09:00:00'),
(3, 1, 'MED-2024B', 20, 14, '2024-07-01', '2026-03-20', '2025-10-05 09:00:00', '2025-10-05 09:00:00');

INSERT INTO stock_in (id, warehouse_id, source_type, operator_id, remark, created_at, updated_at) VALUES
(1, 1, 'PURCHASE', 2, '年初集中采购', '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(2, 3, 'PURCHASE', 2, '医务室补货', '2026-01-12 09:00:00', '2026-01-12 09:00:00');

INSERT INTO stock_in_item (stock_in_id, material_id, batch_no, quantity, production_date, expire_date, created_at, updated_at) VALUES
(1, 1, 'MASK-2026A', 300, '2026-01-15', '2029-01-15', '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(1, 2, 'DIS-2025A', 120, '2025-12-01', '2026-11-30', '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(2, 3, 'MED-2025A', 30, '2025-08-01', '2027-08-01', '2026-01-12 09:00:00', '2026-01-12 09:00:00');

INSERT INTO apply_order (id, dept_id, applicant_id, urgency_level, status, reason, scenario, fast_track, approver_id, approve_remark, approve_time, created_at, updated_at) VALUES
(1, 5, 3, 1, 'RECEIVED', '学院活动防疫物资领取', '大型活动', 0, 4, '同意发放', '2026-02-11 10:00:00', '2026-02-10 09:00:00', '2026-02-12 18:00:00'),
(2, 3, 3, 2, 'OUTBOUND', '暴雨巡查保障', '恶劣天气保障', 1, 4, '紧急快速审批', '2026-03-01 08:20:00', '2026-03-01 08:00:00', '2026-03-01 09:00:00'),
(3, 4, 3, 0, 'SUBMITTED', '医务室常规补充', '日常储备', 0, NULL, NULL, NULL, '2026-03-05 15:00:00', '2026-03-05 15:00:00');

INSERT INTO apply_order_item (apply_order_id, material_id, apply_qty, actual_qty, created_at, updated_at) VALUES
(1, 1, 80, 80, '2026-02-10 09:00:00', '2026-02-11 10:30:00'),
(1, 2, 10, 10, '2026-02-10 09:00:00', '2026-02-11 10:30:00'),
(2, 5, 8, 8, '2026-03-01 08:00:00', '2026-03-01 09:00:00'),
(3, 3, 6, 0, '2026-03-05 15:00:00', '2026-03-05 15:00:00');

INSERT INTO stock_out (id, apply_order_id, warehouse_id, operator_id, remark, created_at, updated_at) VALUES
(1, 1, 1, 2, '审批后出库', '2026-02-11 10:30:00', '2026-02-11 10:30:00'),
(2, 2, 1, 2, '紧急申请优先出库', '2026-03-01 09:00:00', '2026-03-01 09:00:00');

INSERT INTO stock_out_item (stock_out_id, material_id, quantity, created_at, updated_at) VALUES
(1, 1, 80, '2026-02-11 10:30:00', '2026-02-11 10:30:00'),
(1, 2, 10, '2026-02-11 10:30:00', '2026-02-11 10:30:00'),
(2, 5, 8, '2026-03-01 09:00:00', '2026-03-01 09:00:00');

INSERT INTO delivery_task (id, apply_order_id, stock_out_id, receiver_name, receiver_phone, delivery_address, dispatcher_id, status, remark, signed_at, created_at, updated_at) VALUES
(1, 1, 1, '计算机学院李老师', '13800006666', '科学校区计算机学院楼', 6, 'SIGNED', '学院活动物资配送', '2026-02-12 18:00:00', '2026-02-11 11:00:00', '2026-02-12 18:00:00'),
(2, 2, 2, '保卫处值班室', '13800007777', '东风校区学生宿舍区', 6, 'IN_TRANSIT', '暴雨巡查保障配送', NULL, '2026-03-01 09:30:00', '2026-03-01 10:00:00');

INSERT INTO transfer_order (id, from_warehouse_id, to_warehouse_id, status, reason, applicant_id, approver_id, approve_remark, approve_time, created_at, updated_at) VALUES
(1, 1, 2, 'RECEIVED', '东校区储备不足补充', 2, 4, '同意调拨', '2026-02-20 09:00:00', '2026-02-19 10:00:00', '2026-02-20 14:00:00'),
(2, 1, 3, 'APPROVED', '医务室物资补充', 2, 4, '同意', '2026-03-06 09:30:00', '2026-03-06 09:00:00', '2026-03-06 09:30:00');

INSERT INTO transfer_order_item (transfer_order_id, material_id, quantity, created_at, updated_at) VALUES
(1, 1, 20, '2026-02-19 10:00:00', '2026-02-20 14:00:00'),
(2, 3, 5, '2026-03-06 09:00:00', '2026-03-06 09:30:00');

INSERT INTO warning_record (warning_type, material_id, warehouse_id, content, handle_status, handler_id, handle_remark, created_at, updated_at) VALUES
('EXPIRING_SOON', 3, 1, '批次 MED-2024B 距离过期不足30天', 'UNHANDLED', NULL, NULL, '2026-03-07 08:00:00', '2026-03-07 08:00:00'),
('STOCK_LOW', 3, 3, '急救包库存接近安全库存', 'HANDLED', 2, '已提交补货申请', '2026-02-28 09:00:00', '2026-03-01 11:00:00');

INSERT INTO operation_log (operator_id, module, operation, detail, created_at, updated_at) VALUES
(1, 'RBAC', 'INIT', '初始化系统角色与用户', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, 'INVENTORY', 'STOCK_OUT', '出库单:2', '2026-03-01 09:00:00', '2026-03-01 09:00:00');

INSERT INTO ai_analysis_task (id, biz_type, biz_id, request_snapshot, status, result_source, result_json, error_message, created_by, started_at, finished_at, created_at, updated_at) VALUES
(1, 'WARNING', 1, '{"warningId":1,"warningType":"EXPIRING_SOON","materialName":"急救包","warehouseName":"医务室物资仓"}', 'SUCCESS', 'LLM', '{"riskLevel":"HIGH","summary":"临期急救包需要优先处理","possibleCauses":["批次周转慢","近期消耗不足"],"actions":["优先出库临期批次","通知仓管员复核批次周转"],"ownerRole":"WAREHOUSE_ADMIN","deadlineHours":24}', NULL, 2, '2026-05-15 09:10:00', '2026-05-15 09:10:02', '2026-05-15 09:10:00', '2026-05-15 09:10:02'),
(2, 'WARNING', 2, '{"warningId":2,"warningType":"STOCK_LOW","materialName":"急救包","warehouseName":"科学校区总仓"}', 'SUCCESS', 'RULE_FALLBACK', '{"riskLevel":"MEDIUM","summary":"库存接近安全阈值，建议补货或调拨","possibleCauses":["近期集中领用"],"actions":["复核近7天领用记录","安排补货或跨仓调拨"],"ownerRole":"WAREHOUSE_ADMIN","deadlineHours":72}', 'DeepSeek 调用超时，已回退规则分析', 2, '2026-05-15 09:20:00', '2026-05-15 09:20:05', '2026-05-15 09:20:00', '2026-05-15 09:20:05');

INSERT INTO ai_call_log (id, task_id, provider_name, model_name, prompt_template_code, prompt_tokens, completion_tokens, total_tokens, latency_ms, success_flag, error_message, created_at, updated_at) VALUES
(1, 1, 'DEEPSEEK', 'deepseek-v4-flash', 'WARNING_ANALYSIS_V1', 536, 188, 724, 1820, 1, NULL, '2026-05-15 09:10:02', '2026-05-15 09:10:02'),
(2, 2, 'DEEPSEEK', 'deepseek-v4-flash', 'WARNING_ANALYSIS_V1', 0, 0, 0, 0, 0, 'DeepSeek 调用超时，已回退规则分析', '2026-05-15 09:20:05', '2026-05-15 09:20:05');

-- ===================== 校区数据 =====================
INSERT INTO campus (id, campus_name, address, manager, contact_phone, remark) VALUES
(1, '科学校区', '郑州市金水区科学大道136号', '王院长', '0371-86601234', '郑州轻工业大学主校区'),
(2, '东风校区', '郑州市金水区东风路5号', '李院长', '0371-63556789', '郑州轻工业大学东风校区');

-- ===================== 库位数据 =====================
INSERT INTO storage_location (id, location_code, location_name, warehouse_id, capacity, used_qty, status) VALUES
(1, 'KX-A-01', '科学总仓A区1号', 1, 5000, 1200, 'NORMAL'),
(2, 'KX-A-02', '科学总仓A区2号', 1, 5000, 800, 'NORMAL'),
(3, 'KX-B-01', '科学总仓B区1号', 1, 3000, 500, 'NORMAL'),
(4, 'DF-A-01', '东风分仓A区1号', 2, 4000, 600, 'NORMAL'),
(5, 'DF-A-02', '东风分仓A区2号', 2, 4000, 200, 'NORMAL'),
(6, 'YW-01',  '医务室货架1号', 3, 500, 120, 'NORMAL');

-- ===================== 供应商数据 =====================
INSERT INTO supplier (id, supplier_name, contact_person, contact_phone, email, address, supply_scope) VALUES
(1, '华安防护用品有限公司', '赵经理', '13800001111', 'huaan@example.com', '郑州市二七区航海路88号', '口罩、防护服、护目镜'),
(2, '洁安化工有限公司', '钱经理', '13800002222', 'jiean@example.com', '郑州市中原区嵩山路50号', '消毒液、洗手液、酒精'),
(3, '康护医疗器械有限公司', '孙经理', '13800003333', 'kanghu@example.com', '郑州市管城区紫荆山路12号', '急救包、医疗器械、药品'),
(4, '清泉食品有限公司', '李经理', '13800004444', 'qingquan@example.com', '郑州市惠济区花园路100号', '饮用水、储备食品'),
(5, '明锐电子科技有限公司', '周经理', '13800005555', 'mingrui@example.com', '郑州市高新区科学大道66号', '手电筒、对讲机、充电设备');

-- ===================== 事件数据 =====================
INSERT INTO event_record (id, event_title, event_type, event_level, campus_id, location, description, status, reporter_id, handler_id, handle_result, event_time, close_time) VALUES
(1, '教学楼消防演练', 'DRILL', 'NORMAL', 1, '科学校区3号教学楼', '学期例行消防安全疏散演练，需配备物资保障', 'CLOSED', 3, 2, '演练顺利完成，物资全部回收', '2026-02-15 14:00:00', '2026-02-15 16:30:00'),
(2, '暴雨防汛处置', 'NATURAL_DISASTER', 'URGENT', 2, '东风校区学生宿舍区', '气象局发布暴雨红色预警，需紧急调拨防汛物资', 'IN_PROGRESS', 4, 2, NULL, '2026-03-08 07:00:00', NULL),
(3, '食堂食品安全事件', 'SAFETY_INCIDENT', 'CRITICAL', 1, '科学校区第二食堂', '部分学生出现食物不适症状，医务室需紧急补充医疗物资', 'OPEN', 3, NULL, NULL, '2026-03-10 11:30:00', NULL);

-- ===================== 系统配置数据 =====================
INSERT INTO system_config (config_key, config_value, config_name, config_group, remark) VALUES
('safety_stock_threshold', '1.0', '安全库存预警倍数', 'WARNING', '低于安全库存乘以此倍数触发预警'),
('expiry_warn_days', '30', '临期预警天数', 'WARNING', '距过期日期多少天开始预警'),
('abnormal_usage_ratio', '1.5', '异常领用倍率', 'WARNING', '领用量超过月均的此倍率触发预警'),
('backlog_ratio', '3.0', '积压预警倍率', 'WARNING', '库存超过安全库存的此倍率触发积压预警'),
('restock_buffer_days', '30', '补货保障天数', 'SMART', '智能补货时额外保障的天数'),
('forecast_months', '3', '需求预测月数', 'SMART', '默认预测未来几个月的需求'),
('system_name', '校园物资智能管理系统', '系统名称', 'SYSTEM', ''),
('school_name', '郑州轻工业大学', '学校名称', 'SYSTEM', '');

-- ===================== 登录日志数据 =====================
INSERT INTO login_log (user_id, username, login_ip, login_status, login_time, user_agent) VALUES
(1, 'admin', '127.0.0.1', 'SUCCESS', '2026-03-10 08:30:00', 'Mozilla/5.0 Chrome/120'),
(2, 'warehouse', '127.0.0.1', 'SUCCESS', '2026-03-10 09:00:00', 'Mozilla/5.0 Chrome/120'),
(3, 'dept', '192.168.1.50', 'SUCCESS', '2026-03-10 09:15:00', 'Mozilla/5.0 Firefox/115'),
(4, 'approver', '192.168.1.80', 'FAIL', '2026-03-10 09:20:00', 'Mozilla/5.0 Chrome/120');

-- ===================== 通知消息数据 =====================
INSERT INTO notification (title, content, msg_type, target_user_id, is_read, biz_type, biz_id) VALUES
('预警通知', '急救包(MED-2024B)距离过期不足30天，请及时处理', 'WARNING', 2, 0, 'WARNING', 1),
('审批通知', '医务室常规补充申请等待您审批', 'APPROVAL', 4, 0, 'APPLY', 3),
('入库通知', '年初集中采购入库已完成', 'SYSTEM', 1, 1, 'STOCK_IN', 1),
('事件通知', '暴雨防汛处置事件已登记，请关注物资调拨', 'EVENT', 2, 0, 'EVENT', 2);

