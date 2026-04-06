package com.campus.emergency.modules.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.modules.event.entity.EmergencyEvent;
import com.campus.emergency.modules.event.mapper.EmergencyEventMapper;
import com.campus.emergency.modules.log.service.OperationLogService;
import com.campus.emergency.modules.notification.service.NotificationService;
import com.campus.emergency.security.AuthUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmergencyEventService {

    private final EmergencyEventMapper eventMapper;
    private final OperationLogService logService;
    private final NotificationService notificationService;

    public EmergencyEventService(EmergencyEventMapper eventMapper, OperationLogService logService,
                                  NotificationService notificationService) {
        this.eventMapper = eventMapper;
        this.logService = logService;
        this.notificationService = notificationService;
    }

    public List<EmergencyEvent> list(String status, String eventType) {
        LambdaQueryWrapper<EmergencyEvent> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(EmergencyEvent::getStatus, status);
        }
        if (eventType != null && !eventType.isBlank()) {
            qw.eq(EmergencyEvent::getEventType, eventType);
        }
        return eventMapper.selectList(qw.orderByDesc(EmergencyEvent::getEventTime));
    }

    public EmergencyEvent getById(Long id) {
        return eventMapper.selectById(id);
    }

    public void create(EmergencyEvent event) {
        Long uid = AuthUtil.currentUserId();
        event.setReporterId(uid);
        if (event.getStatus() == null) {
            event.setStatus("OPEN");
        }
        eventMapper.insert(event);
        logService.log(uid, "EVENT", "CREATE", "创建应急事件:" + event.getEventTitle());
        // 发送通知
        notificationService.send(null, "应急事件", event.getEventTitle() + " - 请及时处理", "EVENT", event.getId());
    }

    public void handle(Long id, String handleResult) {
        EmergencyEvent event = eventMapper.selectById(id);
        if (event == null) return;
        event.setStatus("IN_PROGRESS");
        event.setHandlerId(AuthUtil.currentUserId());
        event.setHandleResult(handleResult == null ? "" : handleResult);
        eventMapper.updateById(event);
        logService.log(AuthUtil.currentUserId(), "EVENT", "HANDLE", "处理事件:" + id);
    }

    public void close(Long id, String handleResult) {
        EmergencyEvent event = eventMapper.selectById(id);
        if (event == null) return;
        event.setStatus("CLOSED");
        event.setHandleResult(handleResult);
        event.setCloseTime(LocalDateTime.now());
        eventMapper.updateById(event);
        logService.log(AuthUtil.currentUserId(), "EVENT", "CLOSE", "关闭事件:" + id);
    }
}
