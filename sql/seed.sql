USE campus_material;
SET NAMES utf8mb4;

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '系统管理员', '全局管理'),
(2, 'WAREHOUSE_ADMIN', '仓库管理员', '仓库与库存管理'),
(3, 'DEPT_USER', '部门用户', '物资申请与签收'),
(4, 'APPROVER', '审批人员', '审核申请与调拨')
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

INSERT INTO sys_user (id, username, password, real_name, dept_id, role_id, status) VALUES
(1, 'admin', '123456', '系统管理员', 2, 1, 1),
(2, 'warehouse', '123456', '仓库管理员', 2, 2, 1),
(3, 'dept', '123456', '部门申请员', 5, 3, 1),
(4, 'approver', '123456', '审批负责人', 2, 4, 1)
ON DUPLICATE KEY UPDATE
username = VALUES(username),
password = VALUES(password),
real_name = VALUES(real_name),
dept_id = VALUES(dept_id),
role_id = VALUES(role_id),
status = VALUES(status),
deleted = 0;

INSERT INTO material_category (id, category_name, remark) VALUES
(1, '防疫类', '口罩、消毒液等'),
(2, '医疗急救类', '急救包、药品'),
(3, '食品饮水类', '饮用水、压缩饼干'),
(4, '照明通信类', '手电、对讲机'),
(5, '防汛防灾类', '雨衣、沙袋');

INSERT INTO material_info (id, material_code, material_name, category_id, spec, unit, safety_stock, shelf_life_days, supplier, unit_price, remark) VALUES
(1, 'M001', '医用口罩', 1, '50只/盒', '盒', 200, 1095, '华安防护', 35.00, '常备防疫物资'),
(2, 'M002', '84消毒液', 1, '5L/桶', '桶', 60, 365, '洁安化工', 42.50, '重点区域消杀'),
(3, 'M003', '急救包', 2, '标准型', '套', 30, 730, '康护医疗', 98.00, '医务室与宿舍楼备用'),
(4, 'M004', '瓶装饮用水', 3, '550ml*24', '箱', 120, 540, '清泉食品', 30.00, '集中疏散保障'),
(5, 'M005', '强光手电', 4, '可充电', '个', 40, 1825, '明锐电子', 65.00, '停电与夜间巡查');

INSERT INTO warehouse (id, warehouse_name, campus, address, manager) VALUES
(1, '科学校区总仓', '科学校区', '后勤楼B1层', '张老师'),
(2, '东风校区分仓', '东风校区', '体育馆北侧', '李老师'),
(3, '医务室物资仓', '科学校区', '医务室一层', '王医生');

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
