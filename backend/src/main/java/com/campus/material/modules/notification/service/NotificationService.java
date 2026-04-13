package com.campus.material.modules.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.modules.notification.entity.Notification;
import com.campus.material.modules.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /** 按用户查询通知列表（未读优先、最新优先） */
    public List<Notification> listByUser(Long userId) {
        return notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTargetUserId, userId)
                        .orderByAsc(Notification::getIsRead)
                        .orderByDesc(Notification::getId)
        );
    }

    /** 统计用户未读通知数量 */
    public long countUnread(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTargetUserId, userId)
                        .eq(Notification::getIsRead, 0)
        );
    }

    /** 将单条通知标记为已读 */
    public void markRead(Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n != null) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }

    /** 将用户全部未读通知标记为已读 */
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTargetUserId, userId)
                        .eq(Notification::getIsRead, 0)
        );
        for (Notification n : unread) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }

    /** 删除单条通知 */
    public void delete(Long id) {
        notificationMapper.deleteById(id);
    }

    /** 发送通知；当 bizType 为空时默认使用 SYSTEM */
    public void send(Long targetUserId, String title, String content, String bizType, Long bizId) {
        Notification n = new Notification();
        n.setTargetUserId(targetUserId);
        n.setTitle(title);
        n.setContent(content);
        n.setMsgType(bizType != null ? bizType : "SYSTEM");
        n.setBizType(bizType);
        n.setBizId(bizId);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }
}
