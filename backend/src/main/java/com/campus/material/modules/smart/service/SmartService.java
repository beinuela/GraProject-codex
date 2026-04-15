package com.campus.material.modules.smart.service;

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
import java.util.TreeMap;

@Service
public class SmartService {

    private static final DateTimeFormatter MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final JdbcTemplate jdbcTemplate;

    public SmartService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> forecast(Long materialId, int months) {
        if (months <= 0) {
            months = 3;
        }

        Map<String, long[]> monthly = new TreeMap<>();
        jdbcTemplate.query("""
                        select so.created_at as createdAt, soi.quantity as quantity
                        from stock_out_item soi
                        join stock_out so on soi.stock_out_id = so.id and so.deleted=0
                        where soi.deleted=0 and soi.material_id = ? and so.created_at >= ?
                        """,
                (RowCallbackHandler) rs -> mergeMonthlyQty(monthly, rs.getObject("createdAt", LocalDateTime.class), rs.getLong("quantity")),
                materialId,
                LocalDateTime.now().minusMonths(6)
        );

        List<Map<String, Object>> history = new ArrayList<>();
        monthly.forEach((monthKey, qty) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("monthKey", monthKey);
            item.put("qty", qty[0]);
            history.add(item);
        });

        double avg = history.stream()
                .mapToDouble(row -> row.get("qty") == null ? 0 : ((Number) row.get("qty")).doubleValue())
                .average()
                .orElse(0D);

        List<Map<String, Object>> future = new ArrayList<>();
        LocalDate cursor = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        for (int i = 0; i < months; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("monthKey", MONTH_KEY_FORMATTER.format(cursor.plusMonths(i)));
            item.put("predictQty", Math.round(avg));
            future.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("history", history);
        result.put("movingAverage", avg);
        result.put("forecast", future);
        return result;
    }

    public List<Map<String, Object>> replenishmentSuggestions(Integer guaranteeDays) {
        int days = guaranteeDays == null || guaranteeDays <= 0 ? 7 : guaranteeDays;
        String sql = """
                select m.id as materialId,
                       m.material_name as materialName,
                       m.safety_stock as safetyStock,
                       coalesce(inv.totalQty, 0) as currentStock,
                       coalesce(out30.outQty, 0) as outQty30
                from material_info m
                left join (
                    select material_id, sum(current_qty) as totalQty
                    from inventory
                    where deleted=0
                    group by material_id
                ) inv on m.id = inv.material_id
                left join (
                    select soi.material_id, sum(soi.quantity) as outQty
                    from stock_out_item soi
                    join stock_out so on soi.stock_out_id = so.id and so.deleted=0
                    where soi.deleted=0 and so.created_at >= ?
                    group by soi.material_id
                ) out30 on m.id = out30.material_id
                where m.deleted=0
                """;

        List<Map<String, Object>> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", rs.getLong("materialId"));
            item.put("materialName", rs.getString("materialName"));
            item.put("safetyStock", rs.getInt("safetyStock"));
            item.put("currentStock", rs.getInt("currentStock"));
            item.put("outQty30", rs.getInt("outQty30"));
            return item;
        }, LocalDateTime.now().minusDays(30));

        for (Map<String, Object> row : list) {
            int safety = row.get("safetyStock") == null ? 0 : ((Number) row.get("safetyStock")).intValue();
            int stock = row.get("currentStock") == null ? 0 : ((Number) row.get("currentStock")).intValue();
            int out30 = row.get("outQty30") == null ? 0 : ((Number) row.get("outQty30")).intValue();
            double avgDaily = out30 / 30.0;
            int target = (int) Math.ceil(safety + avgDaily * days);
            int suggest = Math.max(0, target - stock);
            row.put("avgDailyUsage", avgDaily);
            row.put("targetStock", target);
            row.put("suggestQty", suggest);
        }

        list.sort((left, right) -> Integer.compare(((Number) right.get("suggestQty")).intValue(), ((Number) left.get("suggestQty")).intValue()));
        return list;
    }

    private void mergeMonthlyQty(Map<String, long[]> monthly, LocalDateTime createdAt, long quantity) {
        if (createdAt == null) {
            return;
        }
        String monthKey = MONTH_KEY_FORMATTER.format(createdAt);
        long[] qty = monthly.computeIfAbsent(monthKey, key -> new long[1]);
        qty[0] += quantity;
    }
}
