package com.campus.emergency.modules.transfer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("transfer_order")
public class TransferOrder extends BaseEntity {
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private String status;
    private String reason;
    private Long applicantId;
    private Long approverId;
    private String approveRemark;
    private LocalDateTime approveTime;
}
