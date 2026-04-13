package com.campus.material.modules.analytics.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> overview() {
        Map<String, Object> map = new HashMap<>();
        map.put("userCount", count("select count(1) from sys_user where deleted=0"));
        map.put("materialCount", count("select count(1) from material_info where deleted=0"));
        map.put("warehouseCount", count("select count(1) from warehouse where deleted=0"));
        map.put("unhandledWarningCount", count("select count(1) from warning_record where deleted=0 and handle_status='UNHANDLED'"));
        map.put("pendingApplyCount", count("select count(1) from apply_order where deleted=0 and status='SUBMITTED'"));
        map.put("pendingTransferCount", count("select count(1) from transfer_order where deleted=0 and status='SUBMITTED'"));
        return map;
    }

    public List<Map<String, Object>> inventoryRatio() {
        String sql = """
                select m.material_name as name, sum(i.current_qty) as value
                from inventory i
                join material_info m on i.material_id = m.id and m.deleted=0
                where i.deleted=0
                group by m.material_name
                order by value desc
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> inboundOutboundTrend() {
        String sql = """
                select month_key,
                       sum(in_qty) as inQty,
                       sum(out_qty) as outQty
                from (
                         select date_format(si.created_at,'%Y-%m') as month_key, sum(sii.quantity) as in_qty, 0 as out_qty
                         from stock_in_item sii
                                  join stock_in si on sii.stock_in_id = si.id and si.deleted=0
                         where sii.deleted=0 and si.created_at >= date_sub(curdate(), interval 6 month)
                         group by date_format(si.created_at,'%Y-%m')
                         union all
                         select date_format(so.created_at,'%Y-%m') as month_key, 0 as in_qty, sum(soi.quantity) as out_qty
                         from stock_out_item soi
                                  join stock_out so on soi.stock_out_id = so.id and so.deleted=0
                         where soi.deleted=0 and so.created_at >= date_sub(curdate(), interval 6 month)
                         group by date_format(so.created_at,'%Y-%m')
                     ) t
                group by month_key
                order by month_key
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> departmentRanking() {
        String sql = """
                select d.dept_name as deptName, sum(aoi.actual_qty) as totalQty
                from apply_order_item aoi
                         join apply_order ao on aoi.apply_order_id = ao.id and ao.deleted=0
                         join sys_dept d on ao.dept_id = d.id and d.deleted=0
                where aoi.deleted=0
                group by d.dept_name
                order by totalQty desc
                limit 10
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> expiryStats() {
        String sql = """
                select m.material_name as materialName,
                       sum(case when b.expire_date < curdate() then b.remain_qty else 0 end) as expiredQty,
                       sum(case when b.expire_date between curdate() and date_add(curdate(), interval 30 day)
                           then b.remain_qty else 0 end) as expiringQty
                from inventory_batch b
                         join material_info m on b.material_id = m.id and m.deleted=0
                where b.deleted=0
                group by m.material_name
                order by expiringQty desc
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> warehouseDistribution() {
        String sql = """
                select w.warehouse_name as warehouseName, sum(i.current_qty) as totalQty
                from inventory i
                         join warehouse w on i.warehouse_id = w.id and w.deleted=0
                where i.deleted=0
                group by w.warehouse_name
                order by totalQty desc
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> emergencyConsumption() {
        String sql = """
                select date_format(so.created_at,'%Y-%m') as monthKey, sum(soi.quantity) as totalQty
                from stock_out_item soi
                         join stock_out so on soi.stock_out_id = so.id and so.deleted=0
                         join apply_order ao on so.apply_order_id = ao.id and ao.deleted=0
                where soi.deleted=0 and ao.urgency_level >= 2 and so.created_at >= date_sub(curdate(), interval 6 month)
                group by date_format(so.created_at,'%Y-%m')
                order by monthKey
                """;
        return jdbcTemplate.queryForList(sql);
    }

    private Long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
