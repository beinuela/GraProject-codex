package com.campus.material.modules.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.modules.event.entity.EventRecord;
import com.campus.material.modules.event.mapper.EventMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.notification.service.NotificationService;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventMapper eventMapper;
    private final OperationLogService logService;
    private final NotificationService notificationService;

    public EventService(EventMapper eventMapper, OperationLogService logService,
                                  NotificationService notificationService) {
        this.eventMapper = eventMapper;
        this.logService = logService;
        this.notificationService = notificationService;
    }

    public List<EventRecord> list(String status, String eventType) {
        LambdaQueryWrapper<EventRecord> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(EventRecord::getStatus, status);
        }
        if (eventType != null && !eventType.isBlank()) {
            qw.eq(EventRecord::getEventType, eventType);
        }
        return eventMapper.selectList(qw.orderByDesc(EventRecord::getEventTime));
    }

    public EventRecord getById(Long id) {
        return eventMapper.selectById(id);
    }

    public void create(EventRecord event) {
        Long uid = AuthUtil.currentUserId();
        event.setReporterId(uid);
        if (event.getStatus() == null) {
            event.setStatus("OPEN");
        }
        eventMapper.insert(event);
        logService.log(uid, "EVENT", "CREATE", "创建物资事件:" + event.getEventTitle());
        // 发送通知
        notificationService.send(null, "物资事件通知", event.getEventTitle() + " - 请及时处理", "EVENT", event.getId());
    }

    public void handle(Long id, String handleResult) {
        EventRecord event = eventMapper.selectById(id);
        if (event == null) return;
        event.setStatus("IN_PROGRESS");
        event.setHandlerId(AuthUtil.currentUserId());
        event.setHandleResult(handleResult == null ? "" : handleResult);
        eventMapper.updateById(event);
        logService.log(AuthUtil.currentUserId(), "EVENT", "HANDLE", "处理事件:" + id);
    }

    public void close(Long id, String handleResult) {
        EventRecord event = eventMapper.selectById(id);
        if (event == null) return;
        event.setStatus("CLOSED");
        event.setHandleResult(handleResult);
        event.setCloseTime(LocalDateTime.now());
        eventMapper.updateById(event);
        logService.log(AuthUtil.currentUserId(), "EVENT", "CLOSE", "关闭事件:" + id);
    }
}
