USE campus_material;
SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(512)
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO v_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;

    IF v_exists = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_column_if_missing('apply_order', 'reserved_warehouse_id', 'BIGINT NULL AFTER fast_track');
CALL add_column_if_missing('material_category', 'category_code', 'VARCHAR(50) NULL AFTER category_name');
CALL add_column_if_missing('warehouse', 'warehouse_code', 'VARCHAR(50) NULL AFTER id');
CALL add_column_if_missing('warehouse', 'campus_id', 'BIGINT NULL AFTER warehouse_name');
CALL add_column_if_missing('warehouse', 'contact_phone', 'VARCHAR(50) NULL AFTER manager');
CALL add_column_if_missing('warehouse', 'status', 'VARCHAR(20) NOT NULL DEFAULT ''NORMAL'' AFTER contact_phone');
CALL add_column_if_missing('warehouse', 'remark', 'VARCHAR(255) NULL AFTER status');

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS delivery_task (
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

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(5, 'PURCHASER', '采购人员', '供应商与入库管理'),
(6, 'DISPATCHER', '调度人员', '配送派单与签收跟踪'),
(7, 'USER', '普通用户', '物资申领与签收')
ON DUPLICATE KEY UPDATE
role_code = VALUES(role_code),
role_name = VALUES(role_name),
description = VALUES(description),
deleted = 0;

INSERT INTO sys_user (id, username, password, real_name, dept_id, role_id, status) VALUES
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

UPDATE material_category
SET category_code = CASE category_name
    WHEN '防疫类' THEN 'CAT-FY'
    WHEN '医疗急救类' THEN 'CAT-YL'
    WHEN '食品饮水类' THEN 'CAT-SP'
    WHEN '照明通信类' THEN 'CAT-ZM'
    WHEN '防汛防灾类' THEN 'CAT-FX'
    ELSE category_code
END
WHERE category_code IS NULL OR category_code = '';

UPDATE warehouse
SET warehouse_code = CASE warehouse_name
    WHEN '科学校区总仓' THEN 'WH-KX-ZC'
    WHEN '东风校区分仓' THEN 'WH-DF-FC'
    WHEN '医务室物资仓' THEN 'WH-YW'
    ELSE warehouse_code
END,
status = COALESCE(NULLIF(status, ''), 'NORMAL')
WHERE deleted = 0;
