package com.campus.material.modules.event.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_record")
public class EventRecord extends BaseEntity {
    private String eventTitle;
    private String eventType;
    private String eventLevel;
    private Long campusId;
    private String location;
    private String description;
    private String status;
    private Long reporterId;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime eventTime;
    private LocalDateTime closeTime;
}
