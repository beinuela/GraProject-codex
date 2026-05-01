package com.campus.material.modules.campus.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("storage_location")
public class StorageLocation extends BaseEntity {
    private String locationCode;
    private String locationName;
    private Long warehouseId;
    private Integer capacity;
    private Integer usedQty;
    private String status;
    private String remark;
}
