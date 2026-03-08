package com.campus.emergency.modules.transfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TransferCreateRequest {
    @NotNull
    private Long fromWarehouseId;
    @NotNull
    private Long toWarehouseId;
    private String reason;

    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long materialId;
        @NotNull
        @Min(1)
        private Integer quantity;
    }
}
