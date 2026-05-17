INSERT INTO sys_role (id, role_code, role_name, description, deleted, version, created_at, updated_at) VALUES
(1, 'ADMIN', '系统管理员', '全局管理', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, 'WAREHOUSE_ADMIN', '仓库管理员', '仓库与库存管理', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, 'DEPT_USER', '部门用户', '物资申请与签收', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(4, 'APPROVER', '审批人员', '审核申请与调拨', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(5, 'PURCHASER', '采购人员', '供应商与入库管理', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(6, 'DISPATCHER', '调度人员', '配送派单与签收跟踪', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(7, 'USER', '普通用户', '物资申领与签收', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO sys_dept (id, dept_name, parent_id, deleted, version, created_at, updated_at) VALUES
(1, '学校本部', NULL, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, '后勤处', 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, '保卫处', 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(4, '医务室', 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(5, '计算机学院', 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO sys_user (id, username, password, real_name, dept_id, role_id, status, deleted, version, created_at, updated_at) VALUES
(1, 'admin', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '系统管理员', 2, 1, 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, 'warehouse', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '仓库管理员', 2, 2, 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, 'dept', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '部门申请员', 5, 3, 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(4, 'approver', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '审批负责人', 2, 4, 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(5, 'purchaser', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '采购专员', 2, 5, 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(6, 'dispatcher', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '配送调度员', 2, 6, 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(7, 'user', '$2a$10$HF0xvRR/pa3b2XcZ2t.DqumBrQCdHWtNN2SUgd/xYhrPjglgru28O', '普通申领人', 5, 7, 1, 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO material_category (id, category_name, category_code, remark, deleted, version, created_at, updated_at) VALUES
(1, '防疫类', 'CAT-FY', '口罩与消杀物资', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, '医疗急救类', 'CAT-YL', '急救包与药械', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, '食品饮水类', 'CAT-SP', '饮用水与应急食品', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(4, '照明通信类', 'CAT-ZM', '夜间照明与通信设备', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO material_info (id, material_code, material_name, category_id, spec, unit, safety_stock, shelf_life_days, supplier, unit_price, remark, deleted, version, created_at, updated_at) VALUES
(1, 'M001', '医用口罩', 1, '50只/盒', '盒', 200, 1095, '华安防护', 35.00, '日常防疫储备', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, 'M002', '84消毒液', 1, '5L/桶', '桶', 60, 365, '洁安化工', 42.50, '重点区域消杀', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, 'M003', '急救包', 2, '标准型', '套', 30, 730, '康护医疗', 98.00, '医务室与宿舍楼备用', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(4, 'M004', '瓶装饮用水', 3, '550ml*24', '箱', 120, 540, '清泉食品', 30.00, '集中疏散保障', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(5, 'M005', '强光手电', 4, '可充电', '个', 40, 1825, '明锐电子', 65.00, '停电与夜间巡查', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO warehouse (id, warehouse_code, warehouse_name, campus_id, campus, address, manager, contact_phone, status, remark, deleted, version, created_at, updated_at) VALUES
(1, 'WH-KX-ZC', '科学校区总仓', 1, '科学校区', '后勤楼B1层', '张老师', '0371-86601201', 'NORMAL', '主校区应急物资总仓', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, 'WH-DF-FC', '东风校区分仓', 2, '东风校区', '体育馆北侧', '李老师', '0371-63556701', 'NORMAL', '东风校区保障仓', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, 'WH-YW', '医务室物资仓', 1, '科学校区', '医务室一层', '王医生', '0371-86601220', 'NORMAL', '医疗急救物资仓', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO inventory (id, material_id, warehouse_id, current_qty, locked_qty, deleted, version, created_at, updated_at) VALUES
(1, 1, 1, 520, 0, 0, 0, '2026-01-02 10:00:00', '2026-04-10 10:00:00'),
(2, 2, 1, 110, 0, 0, 0, '2026-01-02 10:00:00', '2026-04-10 10:00:00'),
(3, 3, 3, 24, 0, 0, 0, '2026-01-12 09:00:00', '2026-04-10 10:00:00'),
(4, 4, 2, 220, 0, 0, 0, '2026-01-18 10:00:00', '2026-04-10 10:00:00'),
(5, 5, 1, 55, 0, 0, 0, '2026-01-05 10:00:00', '2026-04-10 10:00:00'),
(6, 1, 2, 160, 0, 0, 0, '2026-01-20 09:00:00', '2026-04-10 10:00:00'),
(7, 3, 1, 14, 0, 0, 0, '2026-01-05 09:00:00', '2026-04-10 10:00:00');

INSERT INTO inventory_batch (id, material_id, warehouse_id, batch_no, in_qty, remain_qty, production_date, expire_date, deleted, version, created_at, updated_at) VALUES
(1, 1, 1, 'MASK-2025A', 300, 220, '2025-10-01', '2028-10-01', 0, 0, '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(2, 1, 1, 'MASK-2026A', 300, 300, '2026-01-15', '2029-01-15', 0, 0, '2026-02-10 10:00:00', '2026-02-10 10:00:00'),
(3, 2, 1, 'DIS-2025A', 120, 110, '2025-12-01', '2026-11-30', 0, 0, '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(4, 3, 3, 'MED-2024A', 20, 6, '2024-08-01', '2026-04-25', 0, 0, '2025-11-10 09:00:00', '2025-11-10 09:00:00'),
(5, 3, 3, 'MED-2025A', 30, 18, '2025-08-01', '2027-08-01', 0, 0, '2026-01-12 09:00:00', '2026-01-12 09:00:00'),
(6, 4, 2, 'WATER-2025A', 240, 220, '2025-12-20', '2027-06-12', 0, 0, '2026-01-18 10:00:00', '2026-01-18 10:00:00'),
(7, 5, 1, 'LIGHT-2025A', 60, 55, '2025-06-01', '2030-05-31', 0, 0, '2026-01-05 10:00:00', '2026-01-05 10:00:00'),
(8, 1, 2, 'MASK-2025B', 180, 160, '2025-09-15', '2028-09-15', 0, 0, '2026-01-20 09:00:00', '2026-01-20 09:00:00'),
(9, 3, 1, 'MED-2024B', 20, 4, '2024-07-01', '2026-04-10', 0, 0, '2025-10-05 09:00:00', '2025-10-05 09:00:00');

INSERT INTO stock_in (id, warehouse_id, source_type, operator_id, remark, deleted, version, created_at, updated_at) VALUES
(1, 1, 'PURCHASE', 2, '年初集中采购', 0, 0, '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(2, 3, 'PURCHASE', 2, '医务室补货', 0, 0, '2026-01-12 09:00:00', '2026-01-12 09:00:00'),
(3, 2, 'PURCHASE', 2, '东风校区储备补充', 0, 0, '2026-02-18 09:30:00', '2026-02-18 09:30:00');

INSERT INTO stock_in_item (id, stock_in_id, material_id, batch_no, quantity, production_date, expire_date, deleted, version, created_at, updated_at) VALUES
(1, 1, 1, 'MASK-2026A', 300, '2026-01-15', '2029-01-15', 0, 0, '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(2, 1, 2, 'DIS-2025A', 120, '2025-12-01', '2026-11-30', 0, 0, '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(3, 2, 3, 'MED-2025A', 30, '2025-08-01', '2027-08-01', 0, 0, '2026-01-12 09:00:00', '2026-01-12 09:00:00'),
(4, 3, 4, 'WATER-2025A', 240, '2025-12-20', '2027-06-12', 0, 0, '2026-02-18 09:30:00', '2026-02-18 09:30:00');

INSERT INTO apply_order (id, dept_id, applicant_id, urgency_level, status, reason, scenario, fast_track, approver_id, approve_remark, approve_time, deleted, version, created_at, updated_at) VALUES
(1, 5, 3, 1, 'RECEIVED', '学院活动防疫物资领取', '大型活动保障', 0, 4, '同意发放', '2026-02-11 10:00:00', 0, 0, '2026-02-10 09:00:00', '2026-02-12 18:00:00'),
(2, 3, 3, 2, 'OUTBOUND', '暴雨巡查保障', '恶劣天气保障', 1, 4, '紧急快速审批', '2026-03-01 08:20:00', 0, 0, '2026-03-01 08:00:00', '2026-03-01 09:00:00'),
(3, 4, 3, 0, 'SUBMITTED', '医务室常规补充', '日常储备', 0, NULL, NULL, NULL, 0, 0, '2026-04-08 15:00:00', '2026-04-08 15:00:00');

INSERT INTO apply_order_item (id, apply_order_id, material_id, apply_qty, actual_qty, deleted, version, created_at, updated_at) VALUES
(1, 1, 1, 80, 80, 0, 0, '2026-02-10 09:00:00', '2026-02-11 10:30:00'),
(2, 1, 2, 10, 10, 0, 0, '2026-02-10 09:00:00', '2026-02-11 10:30:00'),
(3, 2, 5, 8, 8, 0, 0, '2026-03-01 08:00:00', '2026-03-01 09:00:00'),
(4, 3, 3, 6, 0, 0, 0, '2026-04-08 15:00:00', '2026-04-08 15:00:00');

INSERT INTO stock_out (id, apply_order_id, warehouse_id, operator_id, remark, deleted, version, created_at, updated_at) VALUES
(1, 1, 1, 2, '审批后出库', 0, 0, '2026-02-11 10:30:00', '2026-02-11 10:30:00'),
(2, 2, 1, 2, '紧急申请优先出库', 0, 0, '2026-03-01 09:00:00', '2026-03-01 09:00:00');

INSERT INTO stock_out_item (id, stock_out_id, material_id, quantity, deleted, version, created_at, updated_at) VALUES
(1, 1, 1, 80, 0, 0, '2026-02-11 10:30:00', '2026-02-11 10:30:00'),
(2, 1, 2, 10, 0, 0, '2026-02-11 10:30:00', '2026-02-11 10:30:00'),
(3, 2, 5, 8, 0, 0, '2026-03-01 09:00:00', '2026-03-01 09:00:00');

INSERT INTO delivery_task (id, apply_order_id, stock_out_id, receiver_name, receiver_phone, delivery_address, dispatcher_id, status, remark, signed_at, deleted, version, created_at, updated_at) VALUES
(1, 1, 1, '计算机学院李老师', '13800006666', '科学校区计算机学院楼', 6, 'SIGNED', '学院活动物资配送', '2026-02-12 18:00:00', 0, 0, '2026-02-11 11:00:00', '2026-02-12 18:00:00'),
(2, 2, 2, '保卫处值班室', '13800007777', '东风校区学生宿舍区', 6, 'IN_TRANSIT', '暴雨巡查保障配送', NULL, 0, 0, '2026-03-01 09:30:00', '2026-03-01 10:00:00');

INSERT INTO transfer_order (id, from_warehouse_id, to_warehouse_id, status, reason, applicant_id, approver_id, approve_remark, approve_time, deleted, version, created_at, updated_at) VALUES
(1, 1, 2, 'RECEIVED', '东校区储备不足补充', 2, 4, '同意调拨', '2026-02-20 09:00:00', 0, 0, '2026-02-19 10:00:00', '2026-02-20 14:00:00'),
(2, 1, 3, 'SUBMITTED', '医务室物资补充', 2, NULL, NULL, NULL, 0, 0, '2026-04-10 09:00:00', '2026-04-10 09:00:00');

INSERT INTO transfer_order_item (id, transfer_order_id, material_id, quantity, deleted, version, created_at, updated_at) VALUES
(1, 1, 1, 20, 0, 0, '2026-02-19 10:00:00', '2026-02-20 14:00:00'),
(2, 2, 3, 5, 0, 0, '2026-04-10 09:00:00', '2026-04-10 09:00:00');

INSERT INTO warning_record (id, warning_type, material_id, warehouse_id, content, handle_status, handler_id, handle_remark, deleted, version, created_at, updated_at) VALUES
(1, 'EXPIRING_SOON', 3, 3, '批次 MED-2024A 距离过期不足30天', 'UNHANDLED', NULL, NULL, 0, 0, '2026-04-10 08:00:00', '2026-04-10 08:00:00'),
(2, 'STOCK_LOW', 3, 1, '急救包库存接近安全库存', 'HANDLED', 2, '已提交补货申请', 0, 0, '2026-04-08 09:00:00', '2026-04-09 11:00:00');

INSERT INTO operation_log (id, operator_id, module, operation, detail, deleted, version, created_at, updated_at) VALUES
(1, 3, 'APPLY', 'CREATE', '创建申领单:3', 0, 0, '2026-04-08 15:00:00', '2026-04-08 15:00:00'),
(2, 3, 'APPLY', 'SUBMIT', '提交申领单:3', 0, 0, '2026-04-08 15:10:00', '2026-04-08 15:10:00'),
(3, 2, 'INVENTORY', 'STOCK_OUT', '出库单:2', 0, 0, '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
(4, 2, 'TRANSFER', 'CREATE', '创建调拨单:2', 0, 0, '2026-04-10 09:00:00', '2026-04-10 09:00:00');

INSERT INTO campus (id, campus_name, address, manager, contact_phone, remark, deleted, version, created_at, updated_at) VALUES
(1, '科学校区', '郑州市金水区科学大道136号', '王院长', '0371-86601234', '郑州轻工业大学主校区', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, '东风校区', '郑州市金水区东风路5号', '李院长', '0371-63556789', '郑州轻工业大学东风校区', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO storage_location (id, location_code, location_name, warehouse_id, capacity, used_qty, status, deleted, version, created_at, updated_at) VALUES
(1, 'KX-A-01', '科学总仓A区1号', 1, 5000, 1200, 'NORMAL', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, 'DF-A-01', '东风分仓A区1号', 2, 4000, 600, 'NORMAL', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, 'YW-01', '医务室货架1号', 3, 500, 120, 'NORMAL', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO supplier (id, supplier_name, contact_person, contact_phone, email, address, supply_scope, deleted, version, created_at, updated_at) VALUES
(1, '华安防护用品有限公司', '赵经理', '13800001111', 'huaan@example.com', '郑州市二七区航海路88号', '口罩、防护服、护目镜', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, '洁安化工有限公司', '钱经理', '13800002222', 'jiean@example.com', '郑州市中原区嵩山路50号', '消毒液、洗手液、酒精', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, '康护医疗器械有限公司', '孙经理', '13800003333', 'kanghu@example.com', '郑州市管城区紫荆山路12号', '急救包、医疗器械、药品', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO event_record (id, event_title, event_type, event_level, campus_id, location, description, status, reporter_id, handler_id, handle_result, event_time, close_time, deleted, version, created_at, updated_at) VALUES
(1, '暴雨防汛演练', 'DRILL', 'NORMAL', 1, '科学校区3号教学楼', '演练期间调拨照明和饮水物资', 'CLOSED', 3, 2, '演练顺利完成', '2026-03-15 14:00:00', '2026-03-15 16:30:00', 0, 0, '2026-03-15 14:00:00', '2026-03-15 16:30:00'),
(2, '医务室应急补给', 'MEDICAL', 'URGENT', 1, '科学校区医务室', '需补充急救包与消杀物资', 'IN_PROGRESS', 4, 2, NULL, '2026-04-10 08:30:00', NULL, 0, 0, '2026-04-10 08:30:00', '2026-04-10 08:30:00');

INSERT INTO system_config (id, config_key, config_value, config_name, config_group, remark, deleted, version, created_at, updated_at) VALUES
(1, 'safety_stock_threshold', '1.0', '安全库存预警倍数', 'WARNING', '低于安全库存乘以此倍数触发预警', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, 'expiry_warn_days', '30', '临期预警天数', 'WARNING', '距过期日期多少天开始预警', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, 'restock_buffer_days', '30', '补货保障天数', 'SMART', '智能补货时额外保障的天数', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(4, 'system_name', '校园物资智能管理系统', '系统名称', 'SYSTEM', '', 0, 0, '2026-01-01 09:00:00', '2026-01-01 09:00:00');

INSERT INTO login_log (id, user_id, username, login_ip, login_status, login_time, user_agent, remark, deleted, version, created_at, updated_at) VALUES
(1, 1, 'admin', '127.0.0.1', 'SUCCESS', '2026-04-14 08:30:00', 'Mozilla/5.0 Chrome/120', '', 0, 0, '2026-04-14 08:30:00', '2026-04-14 08:30:00'),
(2, 2, 'warehouse', '127.0.0.1', 'SUCCESS', '2026-04-14 09:00:00', 'Mozilla/5.0 Chrome/120', '', 0, 0, '2026-04-14 09:00:00', '2026-04-14 09:00:00'),
(3, 3, 'dept', '127.0.0.1', 'SUCCESS', '2026-04-14 09:15:00', 'Mozilla/5.0 Chrome/120', '', 0, 0, '2026-04-14 09:15:00', '2026-04-14 09:15:00'),
(4, 4, 'approver', '127.0.0.1', 'SUCCESS', '2026-04-14 09:20:00', 'Mozilla/5.0 Chrome/120', '', 0, 0, '2026-04-14 09:20:00', '2026-04-14 09:20:00');

INSERT INTO notification (id, title, content, msg_type, target_user_id, is_read, biz_type, biz_id, deleted, version, created_at, updated_at) VALUES
(1, '预警通知', '急救包批次距离过期不足30天，请及时处理', 'WARNING', 2, 0, 'WARNING', 1, 0, 0, '2026-04-10 08:10:00', '2026-04-10 08:10:00'),
(2, '审批通知', '医务室常规补充申请等待您审批', 'APPROVAL', 4, 0, 'APPLY', 3, 0, 0, '2026-04-08 15:10:00', '2026-04-08 15:10:00'),
(3, '调拨通知', '医务室物资补充调拨单已提交', 'SYSTEM', 2, 1, 'TRANSFER', 2, 0, 0, '2026-04-10 09:10:00', '2026-04-10 09:10:00');
