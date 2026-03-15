package com.campus.emergency.modules.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfig extends BaseEntity {
    private String configKey;
    private String configValue;
    private String configName;
    private String configGroup;
    private String remark;
}
