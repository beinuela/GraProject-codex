package com.campus.emergency.modules.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustRequest {
    @NotNull
    private Long inventoryId;
    @NotNull
    private Integer actualQty;
    private String remark;
}
