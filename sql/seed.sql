USE campus_emergency;
SET NAMES utf8mb4;

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '系统管理员', '全局管理'),
(2, 'WAREHOUSE_ADMIN', '仓库管理员', '仓库与库存管理'),
(3, 'DEPT_USER', '部门用户', '物资申请与签收'),
(4, 'APPROVER', '审批人员', '审核申请与调拨');

INSERT INTO sys_dept (id, dept_name, parent_id) VALUES
(1, '学校本部', NULL),
(2, '后勤处', 1),
(3, '保卫处', 1),
(4, '医务室', 1),
(5, '计算机学院', 1);

INSERT INTO sys_user (id, username, password, real_name, dept_id, role_id, status) VALUES
(1, 'admin', '123456', '系统管理员', 2, 1, 1),
(2, 'warehouse', '123456', '仓库管理员', 2, 2, 1),
(3, 'dept', '123456', '部门申请员', 5, 3, 1),
(4, 'approver', '123456', '审批负责人', 2, 4, 1);

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
(1, '主校区总仓', '主校区', '后勤楼B1', '张老师'),
(2, '东校区分仓', '东校区', '体育馆北侧', '李老师'),
(3, '医务室应急仓', '主校区', '医务室一层', '王医生');

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
(1, 1, 'PURCHASE', 2, '年初应急采购', '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(2, 3, 'PURCHASE', 2, '医务室补货', '2026-01-12 09:00:00', '2026-01-12 09:00:00');

INSERT INTO stock_in_item (stock_in_id, material_id, batch_no, quantity, production_date, expire_date, created_at, updated_at) VALUES
(1, 1, 'MASK-2026A', 300, '2026-01-15', '2029-01-15', '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(1, 2, 'DIS-2025A', 120, '2025-12-01', '2026-11-30', '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(2, 3, 'MED-2025A', 30, '2025-08-01', '2027-08-01', '2026-01-12 09:00:00', '2026-01-12 09:00:00');

INSERT INTO apply_order (id, dept_id, applicant_id, urgency_level, status, reason, scenario, fast_track, approver_id, approve_remark, approve_time, created_at, updated_at) VALUES
(1, 5, 3, 1, 'RECEIVED', '学院活动防疫物资领取', '大型活动', 0, 4, '同意发放', '2026-02-11 10:00:00', '2026-02-10 09:00:00', '2026-02-12 18:00:00'),
(2, 3, 3, 2, 'OUTBOUND', '暴雨应急巡查保障', '恶劣天气应急', 1, 4, '紧急快速审批', '2026-03-01 08:20:00', '2026-03-01 08:00:00', '2026-03-01 09:00:00'),
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
(2, 1, 3, 'APPROVED', '医务室应急包补充', 2, 4, '同意', '2026-03-06 09:30:00', '2026-03-06 09:00:00', '2026-03-06 09:30:00');

INSERT INTO transfer_order_item (transfer_order_id, material_id, quantity, created_at, updated_at) VALUES
(1, 1, 20, '2026-02-19 10:00:00', '2026-02-20 14:00:00'),
(2, 3, 5, '2026-03-06 09:00:00', '2026-03-06 09:30:00');

INSERT INTO warning_record (warning_type, material_id, warehouse_id, content, handle_status, handler_id, handle_remark, created_at, updated_at) VALUES
('EXPIRING_SOON', 3, 1, '批次 MED-2024B 距离过期不足30天', 'UNHANDLED', NULL, NULL, '2026-03-07 08:00:00', '2026-03-07 08:00:00'),
('STOCK_LOW', 3, 3, '急救包库存接近安全库存', 'HANDLED', 2, '已提交补货申请', '2026-02-28 09:00:00', '2026-03-01 11:00:00');

INSERT INTO operation_log (operator_id, module, operation, detail, created_at, updated_at) VALUES
(1, 'RBAC', 'INIT', '初始化系统角色与用户', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, 'INVENTORY', 'STOCK_OUT', '出库单:2', '2026-03-01 09:00:00', '2026-03-01 09:00:00');
