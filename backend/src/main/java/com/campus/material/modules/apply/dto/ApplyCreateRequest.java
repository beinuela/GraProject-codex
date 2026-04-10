package com.campus.material.modules.apply.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ApplyCreateRequest {
    @NotNull
    private Long deptId;
    @Min(0)
    private Integer urgencyLevel;
    private String reason;
    private String scenario;

    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long materialId;
        @NotNull
        @Min(1)
        private Integer applyQty;
    }
}
