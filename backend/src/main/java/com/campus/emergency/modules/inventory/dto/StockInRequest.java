package com.campus.emergency.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class StockInRequest {
    @NotNull
    private Long warehouseId;
    private String sourceType;
    private String remark;

    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long materialId;
        private String batchNo;
        @NotNull
        @Min(1)
        private Integer quantity;
        private LocalDate productionDate;
        private LocalDate expireDate;
    }
}
