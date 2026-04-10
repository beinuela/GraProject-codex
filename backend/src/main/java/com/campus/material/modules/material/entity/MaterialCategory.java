package com.campus.material.modules.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material_category")
public class MaterialCategory extends BaseEntity {
    private String categoryName;
    private String remark;
}
