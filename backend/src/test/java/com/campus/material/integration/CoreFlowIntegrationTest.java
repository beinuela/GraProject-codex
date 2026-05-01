package com.campus.material.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.modules.apply.entity.ApplyOrder;
import com.campus.material.modules.apply.entity.ApplyOrderItem;
import com.campus.material.modules.apply.mapper.ApplyOrderItemMapper;
import com.campus.material.modules.apply.mapper.ApplyOrderMapper;
import com.campus.material.modules.inventory.entity.InventoryBatch;
import com.campus.material.modules.inventory.entity.Inventory;
import com.campus.material.modules.inventory.mapper.InventoryBatchMapper;
import com.campus.material.modules.inventory.mapper.InventoryMapper;
import com.campus.material.modules.notification.entity.Notification;
import com.campus.material.modules.notification.mapper.NotificationMapper;
import com.campus.material.modules.log.mapper.LoginLogMapper;
import com.campus.material.modules.transfer.entity.TransferOrder;
import com.campus.material.modules.transfer.mapper.TransferOrderMapper;
import com.campus.material.modules.warning.entity.WarningRecord;
import com.campus.material.modules.warning.mapper.WarningRecordMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "security.rate-limit.login-capacity=2",
        "security.rate-limit.login-window-seconds=300",
        "security.rate-limit.refresh-capacity=2",
        "security.rate-limit.refresh-window-seconds=300",
        "security.rate-limit.high-risk-capacity=1",
        "security.rate-limit.high-risk-window-seconds=300"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"/schema-screenshot.sql", "/data-screenshot.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CoreFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplyOrderMapper applyOrderMapper;

    @Autowired
    private ApplyOrderItemMapper applyOrderItemMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private InventoryBatchMapper inventoryBatchMapper;

    @Autowired
    private TransferOrderMapper transferOrderMapper;

    @Autowired
    private WarningRecordMapper warningRecordMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Test
    void loginRefreshAndSecurityHeadersShouldWork() throws Exception {
        Tokens adminTokens = login("admin", "Abc@123456", "198.18.0.10");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .with(request -> {
                            request.setRemoteAddr("198.18.0.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", adminTokens.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String rotatedRefreshToken = readJson(refreshResult).at("/data/refreshToken").asText();
        assertTrue(!rotatedRefreshToken.isBlank() && !rotatedRefreshToken.equals(adminTokens.refreshToken()));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(request -> {
                            request.setRemoteAddr("198.18.0.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", adminTokens.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(request -> {
                            request.setRemoteAddr("198.18.0.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", adminTokens.refreshToken()))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    void loginRateLimitShouldReturnConsistent429() throws Exception {
        String remoteAddr = "198.18.0.20";
        Map<String, Object> payload = Map.of(
                "username", "admin",
                "password", "wrong-password"
        );

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    void pagedApplyAndStockOutFlowShouldUpdateOrderState() throws Exception {
        Tokens deptTokens = login("dept", "Abc@123456", "198.18.0.30");
        MvcResult createResult = authorizedPost("/api/apply", deptTokens.accessToken(), Map.of(
                "deptId", 4,
                "urgencyLevel", 1,
                "reason", "集成测试申领",
                "scenario", "MockMvc 主链路",
                "items", List.of(Map.of("materialId", 3, "applyQty", 5))
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.id").isNumber())
                .andReturn();

        long applyOrderId = readJson(createResult).at("/data/order/id").asLong();

        authorizedPost("/api/apply/" + applyOrderId + "/submit", deptTokens.accessToken(), null)
                .andExpect(status().isOk());

        ApplyOrder submittedOrder = applyOrderMapper.selectById(applyOrderId);
        assertEquals("SUBMITTED", submittedOrder.getStatus());
        assertEquals(3L, submittedOrder.getReservedWarehouseId());

        Inventory reservedInventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, 3L)
                .eq(Inventory::getWarehouseId, 3L)
                .last("limit 1"));
        assertEquals(24, reservedInventory.getCurrentQty());
        assertEquals(5, reservedInventory.getLockedQty());

        Tokens approverTokens = login("approver", "Abc@123456", "198.18.0.31");
        authorizedPost("/api/apply/" + applyOrderId + "/approve", approverTokens.accessToken(), Map.of("remark", "测试审批通过"))
                .andExpect(status().isOk());

        Tokens warehouseTokens = login("warehouse", "Abc@123456", "198.18.0.32");
        authorizedPost("/api/inventory/stock-out", warehouseTokens.accessToken(), Map.of(
                "applyOrderId", applyOrderId,
                "warehouseId", 3,
                "remark", "集成测试出库",
                "items", List.of(Map.of("materialId", 3, "quantity", 5))
        ))
                .andExpect(status().isOk());

        ApplyOrder applyOrder = applyOrderMapper.selectById(applyOrderId);
        assertEquals("OUTBOUND", applyOrder.getStatus());

        Inventory outboundInventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, 3L)
                .eq(Inventory::getWarehouseId, 3L)
                .last("limit 1"));
        assertEquals(19, outboundInventory.getCurrentQty());
        assertEquals(0, outboundInventory.getLockedQty());

        ApplyOrderItem applyOrderItem = applyOrderItemMapper.selectOne(new LambdaQueryWrapper<ApplyOrderItem>()
                .eq(ApplyOrderItem::getApplyOrderId, applyOrderId)
                .eq(ApplyOrderItem::getMaterialId, 3L)
                .last("limit 1"));
        assertEquals(5, applyOrderItem.getActualQty());

        authorizedPost("/api/apply/" + applyOrderId + "/receive", deptTokens.accessToken(), null)
                .andExpect(status().isOk());

        ApplyOrder receivedOrder = applyOrderMapper.selectById(applyOrderId);
        assertEquals("RECEIVED", receivedOrder.getStatus());
        assertNull(receivedOrder.getReservedWarehouseId());

        authorizedGet("/api/apply/list?page=1&size=2", approverTokens.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    void concurrentApplySubmitShouldReserveOnlyLastAvailableItem() throws Exception {
        Tokens deptTokens = login("dept", "Abc@123456", "198.18.0.70");
        setInventorySnapshot(2L, 1L, 1, 0, 1);

        long firstOrderId = createApplyOrder(deptTokens.accessToken(), 4, 2L, 1, "并发申领测试-1");
        long secondOrderId = createApplyOrder(deptTokens.accessToken(), 4, 2L, 1, "并发申领测试-2");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            Future<Integer> firstResult = executor.submit(() -> submitApplyOrder(deptTokens.accessToken(), firstOrderId, "198.18.0.71", startLatch));
            Future<Integer> secondResult = executor.submit(() -> submitApplyOrder(deptTokens.accessToken(), secondOrderId, "198.18.0.72", startLatch));

            startLatch.countDown();

            int firstCode = firstResult.get(10, TimeUnit.SECONDS);
            int secondCode = secondResult.get(10, TimeUnit.SECONDS);

            long successCount = Stream.of(firstCode, secondCode).filter(code -> code == 200).count();
            long businessFailureCount = Stream.of(firstCode, secondCode).filter(code -> code == 409).count();
            assertEquals(1, successCount);
            assertEquals(1, businessFailureCount);
        } finally {
            executor.shutdownNow();
        }

        List<ApplyOrder> orders = applyOrderMapper.selectBatchIds(List.of(firstOrderId, secondOrderId));
        long submittedCount = orders.stream().filter(order -> "SUBMITTED".equals(order.getStatus())).count();
        long draftCount = orders.stream().filter(order -> "DRAFT".equals(order.getStatus())).count();
        long reservedCount = orders.stream().filter(order -> order.getReservedWarehouseId() != null).count();
        assertEquals(1, submittedCount);
        assertEquals(1, draftCount);
        assertEquals(1, reservedCount);

        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, 2L)
                .eq(Inventory::getWarehouseId, 1L)
                .last("limit 1"));
        assertEquals(1, inventory.getCurrentQty());
        assertEquals(1, inventory.getLockedQty());
    }

    @Test
    void transferExecutionShouldMoveInventoryBetweenWarehouses() throws Exception {
        Tokens warehouseTokens = login("warehouse", "Abc@123456", "198.18.0.40");
        MvcResult createResult = authorizedPost("/api/transfer", warehouseTokens.accessToken(), Map.of(
                "fromWarehouseId", 1,
                "toWarehouseId", 2,
                "reason", "集成测试调拨",
                "items", List.of(Map.of("materialId", 1, "quantity", 20))
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.id").isNumber())
                .andReturn();

        long transferOrderId = readJson(createResult).at("/data/order/id").asLong();

        authorizedPost("/api/transfer/" + transferOrderId + "/submit", warehouseTokens.accessToken(), null)
                .andExpect(status().isOk());

        Tokens approverTokens = login("approver", "Abc@123456", "198.18.0.41");
        authorizedPost("/api/transfer/" + transferOrderId + "/approve", approverTokens.accessToken(), Map.of("remark", "测试调拨审批通过"))
                .andExpect(status().isOk());

        authorizedPost("/api/transfer/" + transferOrderId + "/execute", warehouseTokens.accessToken(), null)
                .andExpect(status().isOk());

        TransferOrder transferOrder = transferOrderMapper.selectById(transferOrderId);
        assertEquals("OUTBOUND", transferOrder.getStatus());

        Inventory sourceInventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, 1L)
                .eq(Inventory::getWarehouseId, 1L)
                .last("limit 1"));
        Inventory targetInventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, 1L)
                .eq(Inventory::getWarehouseId, 2L)
                .last("limit 1"));
        assertEquals(500, sourceInventory.getCurrentQty());
        assertEquals(180, targetInventory.getCurrentQty());
    }

    @Test
    void warningHandleAndHighRiskRateLimitShouldWork() throws Exception {
        Tokens warehouseTokens = login("warehouse", "Abc@123456", "198.18.0.50");

        authorizedPost("/api/warning/1/handle", warehouseTokens.accessToken(), Map.of("remark", "已完成测试处理"))
                .andExpect(status().isOk());

        WarningRecord handledWarning = warningRecordMapper.selectById(1L);
        assertEquals("HANDLED", handledWarning.getHandleStatus());

        authorizedPost("/api/warning/scan", warehouseTokens.accessToken(), null)
                .andExpect(status().isOk());

        authorizedPost("/api/warning/scan", warehouseTokens.accessToken(), null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    void pagedInventoryListShouldExposePageEnvelope() throws Exception {
        Tokens adminTokens = login("admin", "Abc@123456", "198.18.0.60");

        authorizedGet("/api/inventory/list?page=1&size=2", adminTokens.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    void unauthenticatedAndForbiddenRequestsShouldReturnExpectedHttpStatus() throws Exception {
        mockMvc.perform(get("/api/auth/token-policy"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        Tokens warehouseTokens = login("warehouse", "Abc@123456", "198.18.0.61");
        authorizedGet("/api/auth/token-policy", warehouseTokens.accessToken())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        Tokens deptTokens = login("dept", "Abc@123456", "198.18.0.62");
        authorizedGet("/api/warehouse/list", deptTokens.accessToken())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void failedAndDisabledLoginShouldCreateLoginAuditRows() throws Exception {
        Tokens adminTokens = login("admin", "Abc@123456", "198.18.0.74");
        long before = loginLogMapper.selectCount(null);

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("198.18.0.73");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        authorizedPost("/api/rbac/users", adminTokens.accessToken(), Map.of(
                "username", "disabledUser",
                "password", "Abc@123456",
                "realName", "禁用账号",
                "deptId", 4,
                "roleId", 4,
                "status", 0
        )).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("198.18.0.75");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "disabledUser", "password", "Abc@123456"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        assertEquals(before + 2, loginLogMapper.selectCount(null));
    }

    @Test
    void eventNotificationShouldBeVisibleAndMarkReadable() throws Exception {
        Tokens adminTokens = login("admin", "Abc@123456", "198.18.0.63");
        long unreadBefore = unreadCount(adminTokens.accessToken());

        authorizedPost("/api/event", adminTokens.accessToken(), Map.of(
                "eventTitle", "集成测试事件通知",
                "eventType", "TEST",
                "eventLevel", "NORMAL",
                "campusId", 1,
                "location", "集成测试楼宇",
                "description", "验证通知中心可见性",
                "eventTime", "2026-04-30T10:00:00"
        ))
                .andExpect(status().isOk());

        long unreadAfterCreate = unreadCount(adminTokens.accessToken());
        assertEquals(unreadBefore + 1, unreadAfterCreate);

        MvcResult listResult = authorizedGet("/api/notification?page=1&size=5", adminTokens.accessToken())
                .andExpect(status().isOk())
                .andReturn();
        JsonNode firstRecord = readJson(listResult).at("/data/records/0");
        long notificationId = firstRecord.path("id").asLong();
        assertTrue(firstRecord.path("title").asText() != null && !firstRecord.path("title").asText().isBlank());

        Notification notification = notificationMapper.selectById(notificationId);
        assertNotNull(notification);
        assertEquals(1L, notification.getTargetUserId());
        assertEquals("EVENT", notification.getBizType());

        authorizedPost("/api/notification/" + notificationId + "/read", adminTokens.accessToken(), null)
                .andExpect(status().isOk());
        assertEquals(unreadBefore, unreadCount(adminTokens.accessToken()));
    }

    @Test
    void duplicateBaseDataShouldReturnConflictWithClearMessage() throws Exception {
        Tokens adminTokens = login("admin", "Abc@123456", "198.18.0.64");
        String suffix = String.valueOf(System.nanoTime());

        authorizedPost("/api/material/category", adminTokens.accessToken(), Map.of(
                "categoryName", "TEST_DUP_CATEGORY_" + suffix,
                "remark", "重复校验测试"
        ))
                .andExpect(status().isOk());
        authorizedPost("/api/material/category", adminTokens.accessToken(), Map.of(
                "categoryName", "TEST_DUP_CATEGORY_" + suffix,
                "remark", "重复校验测试"
        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        authorizedPost("/api/warehouse", adminTokens.accessToken(), Map.of(
                "warehouseName", "TEST_DUP_WAREHOUSE_" + suffix,
                "campus", "科学校区",
                "address", "集成测试地址",
                "manager", "测试管理员"
        ))
                .andExpect(status().isOk());
        authorizedPost("/api/warehouse", adminTokens.accessToken(), Map.of(
                "warehouseName", "TEST_DUP_WAREHOUSE_" + suffix,
                "campus", "科学校区",
                "address", "集成测试地址",
                "manager", "测试管理员"
        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        authorizedPost("/api/rbac/users", adminTokens.accessToken(), Map.of(
                "username", "testdup" + suffix,
                "password", "Abc@123456",
                "realName", "重复校验用户",
                "deptId", 4,
                "roleId", 3,
                "status", 1
        ))
                .andExpect(status().isOk());
        authorizedPost("/api/rbac/users", adminTokens.accessToken(), Map.of(
                "username", "testdup" + suffix,
                "password", "Abc@123456",
                "realName", "重复校验用户",
                "deptId", 4,
                "roleId", 3,
                "status", 1
        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        authorizedPost("/api/rbac/roles", adminTokens.accessToken(), Map.of(
                "roleCode", "TEST_ROLE_" + suffix,
                "roleName", "重复校验角色",
                "description", "重复校验"
        ))
                .andExpect(status().isOk());
        authorizedPost("/api/rbac/roles", adminTokens.accessToken(), Map.of(
                "roleCode", "TEST_ROLE_" + suffix,
                "roleName", "重复校验角色",
                "description", "重复校验"
        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void systemConfigShouldValidateRequiredFieldsAndPreventDuplicateKey() throws Exception {
        Tokens adminTokens = login("admin", "Abc@123456", "198.18.0.641");
        String suffix = String.valueOf(System.nanoTime());
        String configKey = "TEST_CONFIG_" + suffix;

        authorizedPost("/api/config", adminTokens.accessToken(), Map.of(
                "configKey", configKey,
                "configValue", "VALUE_" + suffix,
                "configName", "配置项" + suffix,
                "configGroup", "TEST",
                "remark", "系统配置测试"
        ))
                .andExpect(status().isOk());

        authorizedGet("/api/config?group=TEST", adminTokens.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.configKey=='" + configKey + "')]").isNotEmpty());

        authorizedPost("/api/config", adminTokens.accessToken(), Map.of(
                "configKey", configKey,
                "configValue", "VALUE_DUP",
                "configName", "重复配置",
                "configGroup", "TEST",
                "remark", "重复配置测试"
        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("配置键已存在，请勿重复添加"));

        authorizedPost("/api/config", adminTokens.accessToken(), Map.of(
                "configValue", "VALUE_MISSING_KEY",
                "configName", "缺少键的配置",
                "configGroup", "TEST"
        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("配置键不能为空"));
    }

    @Test
    void validationAndBusinessErrorsShouldUseExpectedHttpStatus() throws Exception {
        Tokens deptTokens = login("dept", "Abc@123456", "198.18.0.65");
        authorizedPost("/api/apply", deptTokens.accessToken(), Map.of(
                "deptId", 4,
                "urgencyLevel", 1,
                "reason", "参数异常测试",
                "scenario", "集成测试",
                "items", List.of(Map.of("materialId", 3, "applyQty", 0))
        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        Tokens warehouseTokens = login("warehouse", "Abc@123456", "198.18.0.66");
        authorizedPost("/api/inventory/stock-out", warehouseTokens.accessToken(), Map.of(
                "warehouseId", 1,
                "remark", "超量出库测试",
                "items", List.of(Map.of("materialId", 1, "quantity", 99999))
        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void smartForecastMissingMaterialShouldReturnBadRequest() throws Exception {
        Tokens adminTokens = login("admin", "Abc@123456", "198.18.0.67");

        authorizedGet("/api/smart/forecast?months=3", adminTokens.accessToken())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("缺少必填参数: materialId"));
    }

    @Test
    void transferRecommendationShouldExcludeTargetWarehouse() throws Exception {
        Tokens warehouseTokens = login("warehouse", "Abc@123456", "198.18.0.68");

        MvcResult result = authorizedGet("/api/transfer/recommend?targetCampus=%E4%B8%9C%E9%A3%8E%E6%A0%A1%E5%8C%BA&materialId=1&qty=1&excludeWarehouseId=2", warehouseTokens.accessToken())
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = readJson(result).path("data");
        assertTrue(records.isArray());
        records.forEach(node -> assertTrue(node.path("warehouseId").asLong() != 2L));
    }

    private Tokens login(String username, String password, String remoteAddr) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", Matchers.containsString("geolocation=()")))
                .andExpect(header().string("Content-Security-Policy", Matchers.containsString("default-src 'none'")))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode root = readJson(result);
        return new Tokens(
                root.at("/data/accessToken").asText(),
                root.at("/data/refreshToken").asText()
        );
    }

    private long createApplyOrder(String accessToken, int deptId, long materialId, int applyQty, String reason) throws Exception {
        MvcResult createResult = authorizedPost("/api/apply", accessToken, Map.of(
                "deptId", deptId,
                "urgencyLevel", 1,
                "reason", reason,
                "scenario", "并发库存保护测试",
                "items", List.of(Map.of("materialId", materialId, "applyQty", applyQty))
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.id").isNumber())
                .andReturn();
        return readJson(createResult).at("/data/order/id").asLong();
    }

    private int submitApplyOrder(String accessToken, long applyOrderId, String remoteAddr, CountDownLatch startLatch) throws Exception {
        startLatch.await(5, TimeUnit.SECONDS);
        MvcResult result = mockMvc.perform(post("/api/apply/" + applyOrderId + "/submit")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        return readJson(result).path("code").asInt();
    }

    private long unreadCount(String accessToken) throws Exception {
        MvcResult result = authorizedGet("/api/notification/unread-count", accessToken)
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }

    private void setInventorySnapshot(long materialId, long warehouseId, int currentQty, int lockedQty, int batchRemainQty) {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .last("limit 1"));
        assertNotNull(inventory);
        inventory.setCurrentQty(currentQty);
        inventory.setLockedQty(lockedQty);
        assertEquals(1, inventoryMapper.updateById(inventory));

        InventoryBatch batch = inventoryBatchMapper.selectOne(new LambdaQueryWrapper<InventoryBatch>()
                .eq(InventoryBatch::getMaterialId, materialId)
                .eq(InventoryBatch::getWarehouseId, warehouseId)
                .orderByAsc(InventoryBatch::getId)
                .last("limit 1"));
        assertNotNull(batch);
        batch.setRemainQty(batchRemainQty);
        assertEquals(1, inventoryBatchMapper.updateById(batch));
    }

    private org.springframework.test.web.servlet.ResultActions authorizedPost(String url, String accessToken, Object body) throws Exception {
        var requestBuilder = post(url)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            requestBuilder.content(json(body));
        }
        return mockMvc.perform(requestBuilder);
    }

    private org.springframework.test.web.servlet.ResultActions authorizedGet(String url, String accessToken) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON));
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record Tokens(String accessToken, String refreshToken) {
    }
}
