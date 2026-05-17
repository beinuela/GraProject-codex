package com.campus.material.modules.llm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.common.BizException;
import com.campus.material.modules.inventory.entity.Inventory;
import com.campus.material.modules.inventory.entity.InventoryBatch;
import com.campus.material.modules.inventory.mapper.InventoryBatchMapper;
import com.campus.material.modules.inventory.mapper.InventoryMapper;
import com.campus.material.modules.llm.config.LlmProperties;
import com.campus.material.modules.llm.dto.AiTaskResponse;
import com.campus.material.modules.llm.dto.DeepSeekChatResult;
import com.campus.material.modules.llm.dto.WarningAiAnalysisResult;
import com.campus.material.modules.llm.entity.AiAnalysisTask;
import com.campus.material.modules.llm.entity.AiCallLog;
import com.campus.material.modules.llm.mapper.AiAnalysisTaskMapper;
import com.campus.material.modules.llm.mapper.AiCallLogMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.material.entity.MaterialInfo;
import com.campus.material.modules.material.mapper.MaterialInfoMapper;
import com.campus.material.modules.warning.entity.WarningRecord;
import com.campus.material.modules.warning.mapper.WarningRecordMapper;
import com.campus.material.modules.warehouse.entity.Warehouse;
import com.campus.material.modules.warehouse.mapper.WarehouseMapper;
import com.campus.material.security.AuthUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AiAnalysisService {

    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> OWNER_ROLES = Set.of("ADMIN", "WAREHOUSE_ADMIN", "APPROVER");

    private final WarningRecordMapper warningRecordMapper;
    private final MaterialInfoMapper materialInfoMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryBatchMapper inventoryBatchMapper;
    private final AiAnalysisTaskMapper aiAnalysisTaskMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final PromptTemplateService promptTemplateService;
    private final DeepSeekLlmClient deepSeekLlmClient;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;
    private final LlmProperties llmProperties;

    public AiAnalysisService(WarningRecordMapper warningRecordMapper,
                             MaterialInfoMapper materialInfoMapper,
                             WarehouseMapper warehouseMapper,
                             InventoryMapper inventoryMapper,
                             InventoryBatchMapper inventoryBatchMapper,
                             AiAnalysisTaskMapper aiAnalysisTaskMapper,
                             AiCallLogMapper aiCallLogMapper,
                             PromptTemplateService promptTemplateService,
                             DeepSeekLlmClient deepSeekLlmClient,
                             ObjectMapper objectMapper,
                             JdbcTemplate jdbcTemplate,
                             OperationLogService operationLogService,
                             LlmProperties llmProperties) {
        this.warningRecordMapper = warningRecordMapper;
        this.materialInfoMapper = materialInfoMapper;
        this.warehouseMapper = warehouseMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryBatchMapper = inventoryBatchMapper;
        this.aiAnalysisTaskMapper = aiAnalysisTaskMapper;
        this.aiCallLogMapper = aiCallLogMapper;
        this.promptTemplateService = promptTemplateService;
        this.deepSeekLlmClient = deepSeekLlmClient;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.operationLogService = operationLogService;
        this.llmProperties = llmProperties;
    }

    public AiTaskResponse<WarningAiAnalysisResult> analyzeWarning(Long warningId) {
        WarningRecord warning = warningRecordMapper.selectById(warningId);
        if (warning == null) {
            throw new BizException("预警记录不存在");
        }

        MaterialInfo material = warning.getMaterialId() == null ? null : materialInfoMapper.selectById(warning.getMaterialId());
        Warehouse warehouse = warning.getWarehouseId() == null ? null : warehouseMapper.selectById(warning.getWarehouseId());
        Inventory inventory = loadInventory(warning.getMaterialId(), warning.getWarehouseId());
        List<InventoryBatch> batches = loadBatches(warning.getMaterialId(), warning.getWarehouseId());

        int stockOutQty7d = queryStockOutQty(warning.getMaterialId(), warning.getWarehouseId(), 7);
        int stockOutQty30d = queryStockOutQty(warning.getMaterialId(), warning.getWarehouseId(), 30);
        int pendingApplyCount = queryPendingApplyCount(warning.getMaterialId());
        int pendingTransferCount = queryPendingTransferCount(warning.getMaterialId());
        int openWarningCount = queryOpenWarningCount(warning.getMaterialId(), warning.getWarehouseId());

        Map<String, Object> snapshot = buildWarningSnapshot(
                warning,
                material,
                warehouse,
                inventory,
                batches,
                stockOutQty7d,
                stockOutQty30d,
                pendingApplyCount,
                pendingTransferCount,
                openWarningCount
        );

        AiAnalysisTask task = new AiAnalysisTask();
        task.setBizType("WARNING");
        task.setBizId(warningId);
        task.setRequestSnapshot(writeJson(snapshot));
        task.setStatus("PENDING");
        task.setResultSource("RULE_FALLBACK");
        task.setCreatedBy(AuthUtil.currentUserId());
        task.setStartedAt(LocalDateTime.now());
        aiAnalysisTaskMapper.insert(task);

        WarningAiAnalysisResult result;
        String source;
        String errorMessage = null;

        if (!llmProperties.isEnabled()) {
            result = buildFallbackResult(warning, material, warehouse, inventory, stockOutQty7d, stockOutQty30d);
            source = "RULE_FALLBACK";
        } else {
            PromptTemplateService.AiPrompt prompt = promptTemplateService.buildWarningAnalysisPrompt(snapshot);
            try {
                DeepSeekChatResult llmResult = deepSeekLlmClient.chatJson(prompt.systemPrompt(), prompt.userPrompt());
                result = normalizeResult(parseWarningResult(llmResult.getContent()), warning, material, inventory);
                source = "LLM";
                insertCallLog(task.getId(), prompt.templateCode(), llmResult, 1, null);
            } catch (Exception ex) {
                result = buildFallbackResult(warning, material, warehouse, inventory, stockOutQty7d, stockOutQty30d);
                source = "RULE_FALLBACK";
                errorMessage = truncate("DeepSeek 调用失败，已回退为规则分析：" + ex.getMessage(), 500);
                insertCallLog(task.getId(), prompt.templateCode(), null, 0, errorMessage);
            }
        }

        task.setStatus("SUCCESS");
        task.setResultSource(source);
        task.setResultJson(writeJson(result));
        task.setErrorMessage(errorMessage);
        task.setFinishedAt(LocalDateTime.now());
        aiAnalysisTaskMapper.updateById(task);

        operationLogService.log(AuthUtil.currentUserId(), "AI", "ANALYZE_WARNING", "执行预警 AI 分析:" + warningId + " 任务:" + task.getId());

        AiTaskResponse<WarningAiAnalysisResult> response = new AiTaskResponse<>();
        response.setTaskId(task.getId());
        response.setStatus(task.getStatus());
        response.setSource(task.getResultSource());
        response.setBizType(task.getBizType());
        response.setResult(result);
        return response;
    }

    private Inventory loadInventory(Long materialId, Long warehouseId) {
        if (materialId == null || warehouseId == null) {
            return null;
        }
        return inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .last("limit 1"));
    }

    private List<InventoryBatch> loadBatches(Long materialId, Long warehouseId) {
        if (materialId == null || warehouseId == null) {
            return List.of();
        }
        return inventoryBatchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .eq(InventoryBatch::getMaterialId, materialId)
                .eq(InventoryBatch::getWarehouseId, warehouseId)
                .gt(InventoryBatch::getRemainQty, 0)
                .orderByAsc(InventoryBatch::getExpireDate)
                .orderByAsc(InventoryBatch::getId)
                .last("limit 5"));
    }

    private Map<String, Object> buildWarningSnapshot(WarningRecord warning,
                                                     MaterialInfo material,
                                                     Warehouse warehouse,
                                                     Inventory inventory,
                                                     List<InventoryBatch> batches,
                                                     int stockOutQty7d,
                                                     int stockOutQty30d,
                                                     int pendingApplyCount,
                                                     int pendingTransferCount,
                                                     int openWarningCount) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        Map<String, Object> warningMap = new LinkedHashMap<>();
        warningMap.put("id", warning.getId());
        warningMap.put("warningType", warning.getWarningType());
        warningMap.put("content", warning.getContent());
        warningMap.put("handleStatus", warning.getHandleStatus());
        warningMap.put("createdAt", warning.getCreatedAt());
        snapshot.put("warning", warningMap);

        Map<String, Object> materialMap = new LinkedHashMap<>();
        materialMap.put("materialId", material == null ? warning.getMaterialId() : material.getId());
        materialMap.put("materialCode", material == null ? null : material.getMaterialCode());
        materialMap.put("materialName", material == null ? null : material.getMaterialName());
        materialMap.put("safetyStock", material == null ? null : material.getSafetyStock());
        materialMap.put("shelfLifeDays", material == null ? null : material.getShelfLifeDays());
        snapshot.put("material", materialMap);

        Map<String, Object> warehouseMap = new LinkedHashMap<>();
        warehouseMap.put("warehouseId", warehouse == null ? warning.getWarehouseId() : warehouse.getId());
        warehouseMap.put("warehouseName", warehouse == null ? null : warehouse.getWarehouseName());
        warehouseMap.put("campus", warehouse == null ? null : warehouse.getCampus());
        snapshot.put("warehouse", warehouseMap);

        Map<String, Object> inventoryMap = new LinkedHashMap<>();
        inventoryMap.put("currentQty", inventory == null ? null : inventory.getCurrentQty());
        inventoryMap.put("lockedQty", inventory == null ? null : inventory.getLockedQty());
        inventoryMap.put("safetyGap", calculateSafetyGap(material, inventory));
        snapshot.put("inventory", inventoryMap);

        Map<String, Object> usageMap = new LinkedHashMap<>();
        usageMap.put("stockOutQty7d", stockOutQty7d);
        usageMap.put("stockOutQty30d", stockOutQty30d);
        usageMap.put("avgWeeklyQty30d", roundToTwoDecimal(stockOutQty30d / 4.0));
        snapshot.put("recentUsage", usageMap);

        Map<String, Object> pressureMap = new LinkedHashMap<>();
        pressureMap.put("pendingApplyCount", pendingApplyCount);
        pressureMap.put("pendingTransferCount", pendingTransferCount);
        pressureMap.put("openWarningCount", openWarningCount);
        snapshot.put("openPressure", pressureMap);

        List<Map<String, Object>> batchRows = new ArrayList<>();
        for (InventoryBatch batch : batches) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("batchNo", batch.getBatchNo());
            row.put("remainQty", batch.getRemainQty());
            row.put("productionDate", batch.getProductionDate());
            row.put("expireDate", batch.getExpireDate());
            batchRows.add(row);
        }
        snapshot.put("batches", batchRows);
        return snapshot;
    }

    private WarningAiAnalysisResult parseWarningResult(String content) {
        try {
            return objectMapper.readValue(content, WarningAiAnalysisResult.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析预警分析结果失败", ex);
        }
    }

    private WarningAiAnalysisResult normalizeResult(WarningAiAnalysisResult parsed,
                                                    WarningRecord warning,
                                                    MaterialInfo material,
                                                    Inventory inventory) {
        WarningAiAnalysisResult normalized = new WarningAiAnalysisResult();
        normalized.setRiskLevel(normalizeRiskLevel(parsed == null ? null : parsed.getRiskLevel(), warning, inventory, material));
        normalized.setSummary(firstNonBlank(parsed == null ? null : parsed.getSummary(), warning.getContent()));
        normalized.setPossibleCauses(normalizeItems(parsed == null ? null : parsed.getPossibleCauses(), defaultCauses(warning)));
        normalized.setActions(normalizeItems(parsed == null ? null : parsed.getActions(), defaultActions(warning, material)));
        normalized.setOwnerRole(normalizeOwnerRole(parsed == null ? null : parsed.getOwnerRole()));
        normalized.setDeadlineHours(normalizeDeadlineHours(parsed == null ? null : parsed.getDeadlineHours(), normalized.getRiskLevel()));
        return normalized;
    }

    private WarningAiAnalysisResult buildFallbackResult(WarningRecord warning,
                                                        MaterialInfo material,
                                                        Warehouse warehouse,
                                                        Inventory inventory,
                                                        int stockOutQty7d,
                                                        int stockOutQty30d) {
        WarningAiAnalysisResult result = new WarningAiAnalysisResult();
        String materialName = material == null ? "该物资" : material.getMaterialName();
        String warehouseName = warehouse == null ? "当前仓" : warehouse.getWarehouseName();
        result.setRiskLevel(normalizeRiskLevel(null, warning, inventory, material));
        result.setSummary(buildFallbackSummary(warning, materialName, warehouseName, inventory, stockOutQty7d, stockOutQty30d));
        result.setPossibleCauses(defaultCauses(warning));
        result.setActions(defaultActions(warning, material));
        result.setOwnerRole("WAREHOUSE_ADMIN");
        result.setDeadlineHours(normalizeDeadlineHours(null, result.getRiskLevel()));
        return result;
    }

    private String buildFallbackSummary(WarningRecord warning,
                                        String materialName,
                                        String warehouseName,
                                        Inventory inventory,
                                        int stockOutQty7d,
                                        int stockOutQty30d) {
        return switch (warning.getWarningType()) {
            case "STOCK_LOW" -> materialName + " 在 " + warehouseName + " 已触发低库存预警，建议优先补货或调拨，避免影响近期领用。";
            case "STOCK_BACKLOG" -> materialName + " 在 " + warehouseName + " 库存偏高，建议复核领用节奏并优化周转。";
            case "EXPIRING_SOON" -> materialName + " 在 " + warehouseName + " 存在临期批次，建议优先出库并安排重点消耗。";
            case "EXPIRED" -> materialName + " 在 " + warehouseName + " 已出现过期库存，建议立即隔离并启动处置流程。";
            case "ABNORMAL_USAGE" -> materialName + " 近 7 天出库量异常，本周出库 " + stockOutQty7d + "，近 30 天累计出库 " + stockOutQty30d + "，建议排查异常领用原因。";
            default -> warning.getContent();
        };
    }

    private List<String> defaultCauses(WarningRecord warning) {
        return switch (warning.getWarningType()) {
            case "STOCK_LOW" -> List.of("近期集中领用导致库存下降", "补货周期或调拨响应偏慢");
            case "STOCK_BACKLOG" -> List.of("采购或入库节奏高于实际消耗", "安全库存阈值设置偏低或物资周转变慢");
            case "EXPIRING_SOON" -> List.of("批次周转顺序未充分前置", "近阶段实际消耗低于预期");
            case "EXPIRED" -> List.of("临期批次未及时消耗或调拨", "效期监控与现场处置不及时");
            case "ABNORMAL_USAGE" -> List.of("短期突发事件或集中申领", "存在异常领用或统计波动");
            default -> List.of("当前预警需要结合业务记录进一步核查");
        };
    }

    private List<String> defaultActions(WarningRecord warning, MaterialInfo material) {
        String materialName = material == null ? "该物资" : material.getMaterialName();
        return switch (warning.getWarningType()) {
            case "STOCK_LOW" -> List.of(
                    "复核 " + materialName + " 的最近 7 天领用记录和当前可用库存",
                    "优先执行补货或跨仓调拨，确保安全库存恢复",
                    "必要时临时提高审批与出库优先级"
            );
            case "STOCK_BACKLOG" -> List.of(
                    "核查库存积压批次和历史采购节奏",
                    "优先安排调拨或前置消耗高周转仓库",
                    "复核安全库存和采购阈值是否需要调整"
            );
            case "EXPIRING_SOON" -> List.of(
                    "优先按临期批次执行出库和调拨",
                    "通知仓管员对临期批次进行重点标记",
                    "评估是否需要组织专项清理和集中消耗"
            );
            case "EXPIRED" -> List.of(
                    "立即隔离过期库存并暂停流转",
                    "登记处置记录并上报管理人员",
                    "复盘效期预警与仓储周转执行情况"
            );
            case "ABNORMAL_USAGE" -> List.of(
                    "核对异常时段的申领、审批和出库记录",
                    "联系相关部门确认是否存在突发集中消耗",
                    "必要时补充安全库存并监控后续 3 天波动"
            );
            default -> List.of("由仓库管理员复核预警详情并制定处置方案");
        };
    }

    private String normalizeRiskLevel(String riskLevel, WarningRecord warning, Inventory inventory, MaterialInfo material) {
        String normalized = riskLevel == null ? "" : riskLevel.trim().toUpperCase(Locale.ROOT);
        if (RISK_LEVELS.contains(normalized)) {
            return normalized;
        }

        return switch (warning.getWarningType()) {
            case "EXPIRED" -> "CRITICAL";
            case "STOCK_LOW" -> {
                int currentQty = inventory == null || inventory.getCurrentQty() == null ? 0 : inventory.getCurrentQty();
                int safetyStock = material == null || material.getSafetyStock() == null ? 0 : material.getSafetyStock();
                yield currentQty <= Math.max(0, safetyStock / 2) ? "HIGH" : "MEDIUM";
            }
            case "ABNORMAL_USAGE" -> "HIGH";
            case "EXPIRING_SOON" -> "MEDIUM";
            case "STOCK_BACKLOG" -> "LOW";
            default -> "MEDIUM";
        };
    }

    private String normalizeOwnerRole(String ownerRole) {
        String normalized = ownerRole == null ? "" : ownerRole.trim().toUpperCase(Locale.ROOT);
        return OWNER_ROLES.contains(normalized) ? normalized : "WAREHOUSE_ADMIN";
    }

    private Integer normalizeDeadlineHours(Integer deadlineHours, String riskLevel) {
        if (deadlineHours != null && deadlineHours >= 4 && deadlineHours <= 168) {
            return deadlineHours;
        }
        return switch (riskLevel) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 24;
            case "MEDIUM" -> 72;
            default -> 120;
        };
    }

    private List<String> normalizeItems(List<String> items, List<String> defaults) {
        List<String> normalized = new ArrayList<>();
        if (items != null) {
            for (String item : items) {
                if (item != null && !item.isBlank()) {
                    normalized.add(item.trim());
                }
            }
        }
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return defaults;
    }

    private int calculateSafetyGap(MaterialInfo material, Inventory inventory) {
        int safetyStock = material == null || material.getSafetyStock() == null ? 0 : material.getSafetyStock();
        int currentQty = inventory == null || inventory.getCurrentQty() == null ? 0 : inventory.getCurrentQty();
        return currentQty - safetyStock;
    }

    private double roundToTwoDecimal(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return fallback;
    }

    private int queryStockOutQty(Long materialId, Long warehouseId, int days) {
        if (materialId == null) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("""
                select coalesce(sum(soi.quantity), 0)
                from stock_out_item soi
                join stock_out so on soi.stock_out_id = so.id and so.deleted = 0
                where soi.deleted = 0
                  and soi.material_id = ?
                  and so.created_at >= ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(materialId);
        args.add(LocalDateTime.now().minusDays(days));
        if (warehouseId != null) {
            sql.append(" and so.warehouse_id = ?");
            args.add(warehouseId);
        }
        Integer value = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return value == null ? 0 : value;
    }

    private int queryPendingApplyCount(Long materialId) {
        if (materialId == null) {
            return 0;
        }
        Integer value = jdbcTemplate.queryForObject("""
                        select count(*)
                        from apply_order_item aoi
                        join apply_order ao on aoi.apply_order_id = ao.id and ao.deleted = 0
                        where aoi.deleted = 0
                          and aoi.material_id = ?
                          and ao.status in ('SUBMITTED', 'APPROVED', 'OUTBOUND')
                        """,
                Integer.class,
                materialId
        );
        return value == null ? 0 : value;
    }

    private int queryPendingTransferCount(Long materialId) {
        if (materialId == null) {
            return 0;
        }
        Integer value = jdbcTemplate.queryForObject("""
                        select count(*)
                        from transfer_order_item toi
                        join transfer_order tor on toi.transfer_order_id = tor.id and tor.deleted = 0
                        where toi.deleted = 0
                          and toi.material_id = ?
                          and tor.status in ('SUBMITTED', 'APPROVED', 'OUTBOUND')
                        """,
                Integer.class,
                materialId
        );
        return value == null ? 0 : value;
    }

    private int queryOpenWarningCount(Long materialId, Long warehouseId) {
        StringBuilder sql = new StringBuilder("""
                select count(*)
                from warning_record
                where deleted = 0
                  and handle_status = 'UNHANDLED'
                """);
        List<Object> args = new ArrayList<>();
        if (materialId != null) {
            sql.append(" and material_id = ?");
            args.add(materialId);
        }
        if (warehouseId != null) {
            sql.append(" and warehouse_id = ?");
            args.add(warehouseId);
        }
        Integer value = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return value == null ? 0 : value;
    }

    private void insertCallLog(Long taskId,
                               String promptTemplateCode,
                               DeepSeekChatResult llmResult,
                               int successFlag,
                               String errorMessage) {
        AiCallLog callLog = new AiCallLog();
        callLog.setTaskId(taskId);
        callLog.setProviderName("DEEPSEEK");
        callLog.setModelName(llmResult == null ? llmProperties.getModel() : llmResult.getModelName());
        callLog.setPromptTemplateCode(promptTemplateCode);
        callLog.setPromptTokens(llmResult == null ? 0 : llmResult.getPromptTokens());
        callLog.setCompletionTokens(llmResult == null ? 0 : llmResult.getCompletionTokens());
        callLog.setTotalTokens(llmResult == null ? 0 : llmResult.getTotalTokens());
        callLog.setLatencyMs(llmResult == null ? 0L : llmResult.getLatencyMs());
        callLog.setSuccessFlag(successFlag);
        callLog.setErrorMessage(errorMessage);
        aiCallLogMapper.insert(callLog);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化 AI 数据失败", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
