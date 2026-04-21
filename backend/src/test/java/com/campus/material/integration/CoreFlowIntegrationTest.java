package com.campus.material.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.modules.apply.entity.ApplyOrder;
import com.campus.material.modules.apply.entity.ApplyOrderItem;
import com.campus.material.modules.apply.mapper.ApplyOrderItemMapper;
import com.campus.material.modules.apply.mapper.ApplyOrderMapper;
import com.campus.material.modules.inventory.entity.Inventory;
import com.campus.material.modules.inventory.mapper.InventoryMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private TransferOrderMapper transferOrderMapper;

    @Autowired
    private WarningRecordMapper warningRecordMapper;

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
                .andExpect(status().isOk())
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk())
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

        ApplyOrderItem applyOrderItem = applyOrderItemMapper.selectOne(new LambdaQueryWrapper<ApplyOrderItem>()
                .eq(ApplyOrderItem::getApplyOrderId, applyOrderId)
                .eq(ApplyOrderItem::getMaterialId, 3L)
                .last("limit 1"));
        assertEquals(5, applyOrderItem.getActualQty());

        authorizedGet("/api/apply/list?page=1&size=2", approverTokens.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.total").isNumber());
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
