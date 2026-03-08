CREATE DATABASE IF NOT EXISTS campus_emergency DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE campus_emergency;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS warning_record;
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

CREATE TABLE material_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    remark VARCHAR(255),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
    warehouse_name VARCHAR(100) NOT NULL,
    campus VARCHAR(100),
    address VARCHAR(255),
    manager VARCHAR(100),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
    INDEX idx_batch_expire (expire_date)
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
    INDEX idx_stock_out_item_material (material_id)
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
    INDEX idx_warning_type (warning_type)
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
    INDEX idx_log_module (module)
);

SET FOREIGN_KEY_CHECKS = 1;
