package com.campus.material.modules.smart.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SmartService {

    private final JdbcTemplate jdbcTemplate;

    public SmartService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> forecast(Long materialId, int months) {
        if (months <= 0) {
            months = 3;
        }
        String sql = """
                select date_format(so.created_at,'%Y-%m') as monthKey, sum(soi.quantity) as qty
                from stock_out_item soi
                         join stock_out so on soi.stock_out_id = so.id and so.deleted=0
                where soi.deleted=0 and soi.material_id = ? and so.created_at >= date_sub(curdate(), interval 6 month)
                group by date_format(so.created_at,'%Y-%m')
                order by monthKey
                """;
        List<Map<String, Object>> history = jdbcTemplate.queryForList(sql, materialId);
        double avg = history.stream()
                .mapToDouble(row -> row.get("qty") == null ? 0 : ((Number) row.get("qty")).doubleValue())
                .average().orElse(0D);

        List<Map<String, Object>> future = new ArrayList<>();
        LocalDate cursor = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 0; i < months; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("monthKey", formatter.format(cursor.plusMonths(i)));
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
                       ifnull(inv.totalQty,0) as currentStock,
                       ifnull(out30.outQty,0) as outQty30
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
                    where soi.deleted=0 and so.created_at >= date_sub(curdate(), interval 30 day)
                    group by soi.material_id
                ) out30 on m.id = out30.material_id
                where m.deleted=0
                """;
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
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
        list.sort((a, b) -> Integer.compare(((Number) b.get("suggestQty")).intValue(), ((Number) a.get("suggestQty")).intValue()));
        return list;
    }
}
