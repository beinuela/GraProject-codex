package com.campus.material.modules.analytics.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Service
public class AnalyticsService {

    private static final DateTimeFormatter MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

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
                select m.material_name as name, sum(i.current_qty) as totalQty
                from inventory i
                join material_info m on i.material_id = m.id and m.deleted=0
                where i.deleted=0
                group by m.material_name
                order by sum(i.current_qty) desc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", rs.getString("name"));
            item.put("value", rs.getLong("totalQty"));
            return item;
        });
    }

    public List<Map<String, Object>> inboundOutboundTrend() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        Map<String, long[]> monthly = new TreeMap<>();

        jdbcTemplate.query("""
                        select si.created_at as createdAt, sii.quantity as quantity
                        from stock_in_item sii
                        join stock_in si on sii.stock_in_id = si.id and si.deleted=0
                        where sii.deleted=0 and si.created_at >= ?
                        """,
                (RowCallbackHandler) rs -> mergeMonthlyQty(monthly, rs.getObject("createdAt", LocalDateTime.class), rs.getLong("quantity"), 0),
                cutoff
        );

        jdbcTemplate.query("""
                        select so.created_at as createdAt, soi.quantity as quantity
                        from stock_out_item soi
                        join stock_out so on soi.stock_out_id = so.id and so.deleted=0
                        where soi.deleted=0 and so.created_at >= ?
                        """,
                (RowCallbackHandler) rs -> mergeMonthlyQty(monthly, rs.getObject("createdAt", LocalDateTime.class), rs.getLong("quantity"), 1),
                cutoff
        );

        List<Map<String, Object>> result = new ArrayList<>();
        monthly.forEach((monthKey, qty) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month_key", monthKey);
            item.put("inQty", qty[0]);
            item.put("outQty", qty[1]);
            result.add(item);
        });
        return result;
    }

    public List<Map<String, Object>> departmentRanking() {
        String sql = """
                select d.dept_name as deptName, sum(aoi.actual_qty) as totalQty
                from apply_order_item aoi
                join apply_order ao on aoi.apply_order_id = ao.id and ao.deleted=0
                join sys_dept d on ao.dept_id = d.id and d.deleted=0
                where aoi.deleted=0
                group by d.dept_name
                order by sum(aoi.actual_qty) desc
                limit 10
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deptName", rs.getString("deptName"));
            item.put("totalQty", rs.getLong("totalQty"));
            return item;
        });
    }

    public List<Map<String, Object>> expiryStats() {
        LocalDate today = LocalDate.now();
        LocalDate expiringDeadline = today.plusDays(30);
        Map<String, long[]> totals = new LinkedHashMap<>();

        jdbcTemplate.query("""
                        select m.material_name as materialName, b.expire_date as expireDate, b.remain_qty as remainQty
                        from inventory_batch b
                        join material_info m on b.material_id = m.id and m.deleted=0
                        where b.deleted=0
                        """,
                (RowCallbackHandler) rs -> {
                    String materialName = rs.getString("materialName");
                    LocalDate expireDate = rs.getObject("expireDate", LocalDate.class);
                    long remainQty = rs.getLong("remainQty");
                    long[] qty = totals.computeIfAbsent(materialName, key -> new long[2]);
                    if (expireDate != null && expireDate.isBefore(today)) {
                        qty[0] += remainQty;
                    }
                    if (expireDate != null && !expireDate.isBefore(today) && !expireDate.isAfter(expiringDeadline)) {
                        qty[1] += remainQty;
                    }
                }
        );

        return totals.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue()[1], left.getValue()[1]))
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("materialName", entry.getKey());
                    item.put("expiredQty", entry.getValue()[0]);
                    item.put("expiringQty", entry.getValue()[1]);
                    return item;
                })
                .toList();
    }

    public List<Map<String, Object>> warehouseDistribution() {
        String sql = """
                select w.warehouse_name as warehouseName, sum(i.current_qty) as totalQty
                from inventory i
                join warehouse w on i.warehouse_id = w.id and w.deleted=0
                where i.deleted=0
                group by w.warehouse_name
                order by sum(i.current_qty) desc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("warehouseName", rs.getString("warehouseName"));
            item.put("totalQty", rs.getLong("totalQty"));
            return item;
        });
    }

    public List<Map<String, Object>> emergencyConsumption() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        Map<String, long[]> monthly = new TreeMap<>();

        jdbcTemplate.query("""
                        select so.created_at as createdAt, soi.quantity as quantity
                        from stock_out_item soi
                        join stock_out so on soi.stock_out_id = so.id and so.deleted=0
                        join apply_order ao on so.apply_order_id = ao.id and ao.deleted=0
                        where soi.deleted=0 and ao.urgency_level >= 2 and so.created_at >= ?
                        """,
                (RowCallbackHandler) rs -> mergeMonthlyQty(monthly, rs.getObject("createdAt", LocalDateTime.class), rs.getLong("quantity"), 0),
                cutoff
        );

        List<Map<String, Object>> result = new ArrayList<>();
        monthly.forEach((monthKey, qty) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("monthKey", monthKey);
            item.put("totalQty", qty[0]);
            result.add(item);
        });
        return result;
    }

    private Long count(String sql) {
        Long value = jdbcTemplate.queryForObject(Objects.requireNonNull(sql), Long.class);
        return value == null ? 0L : value;
    }

    private void mergeMonthlyQty(Map<String, long[]> monthly, LocalDateTime createdAt, long quantity, int index) {
        if (createdAt == null) {
            return;
        }
        String monthKey = MONTH_KEY_FORMATTER.format(createdAt);
        long[] qty = monthly.computeIfAbsent(monthKey, key -> new long[2]);
        qty[index] += quantity;
    }
}
