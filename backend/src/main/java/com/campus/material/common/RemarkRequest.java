package com.campus.material.common;

import lombok.Data;

/**
 * 通用备注请求体，适用于审批、驳回、处理等需要附带备注的操作。
 */
@Data
public class RemarkRequest {
    private String remark;
}
