package com.campus.emergency.modules.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.modules.notification.entity.Notification;
import com.campus.emergency.modules.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /** 闁哄被鍎撮妤呭箰閸パ呮毎闁活潿鍔嶉崺娑㈡儍閸曨垪鍋撳杈╁弨闁告帗顨夐妴?*/
    public List<Notification> listByUser(Long userId) {
        return notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTargetUserId, userId)
                        .orderByAsc(Notification::getIsRead)
                        .orderByDesc(Notification::getId)
        );
    }

    /** 缂備胶鍠曢鎼佸嫉椤忓浂鍤㈤柡浣峰嵆閸?*/
    public long countUnread(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTargetUserId, userId)
                        .eq(Notification::getIsRead, 0)
        );
    }

    /** 闁哄秴娲╅鍥ь啅閼奸鍤?*/
    public void markRead(Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n != null) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }

    /** 闁稿繈鍔戦崕鏉戭啅閼奸鍤?*/
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

    /** 闁告帞濞€濞呭酣鏌呭杈╁弨 */
    public void delete(Long id) {
        notificationMapper.deleteById(id);
    }

    /** 闁告瑦鍨块埀顑跨窔閳ь剚姘ㄩ悡锟犳晬閸х浛rgetUserId 濞?null 闁哄啯鍎奸妴鍐矆閸濆嫮鐣柟缁㈠幘缁即骞嶉埀顒勫嫉婢跺鐪介柨?*/
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
