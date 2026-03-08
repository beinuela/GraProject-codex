package com.campus.emergency.modules.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material_info")
public class MaterialInfo extends BaseEntity {
    private String materialCode;
    private String materialName;
    private Long categoryId;
    private String spec;
    private String unit;
    private Integer safetyStock;
    private Integer shelfLifeDays;
    private String supplier;
    private BigDecimal unitPrice;
    private String remark;
}
