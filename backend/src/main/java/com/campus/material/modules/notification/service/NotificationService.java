package com.campus.material.modules.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.modules.notification.entity.Notification;
import com.campus.material.modules.notification.mapper.NotificationMapper;
import com.campus.material.modules.rbac.entity.SysRole;
import com.campus.material.modules.rbac.entity.SysUser;
import com.campus.material.modules.rbac.mapper.SysRoleMapper;
import com.campus.material.modules.rbac.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public NotificationService(NotificationMapper notificationMapper, SysUserMapper userMapper, SysRoleMapper roleMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    /** 按用户查询通知列表（未读优先、最新优先） */
    public PageResult<Notification> listByUser(Long userId, PageQuery pageQuery) {
        Page<Notification> page = notificationMapper.selectPage(
                new Page<>(pageQuery.getPage(), pageQuery.getSize()),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTargetUserId, userId)
                        .orderByAsc(Notification::getIsRead)
                        .orderByDesc(Notification::getId)
        );
        return PageResult.from(page);
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
    public void markRead(Long id, Long userId) {
        Notification n = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getTargetUserId, userId)
                .last("limit 1"));
        if (n != null) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }

    /** 将用户全部未读通知标记为已读 */
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getTargetUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    /** 删除单条通知 */
    public void delete(Long id, Long userId) {
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getTargetUserId, userId));
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

    public void sendToRoleCodes(Collection<String> roleCodes, Long extraUserId,
                                String title, String content, String bizType, Long bizId) {
        LinkedHashSet<Long> targetUserIds = new LinkedHashSet<>();
        if (extraUserId != null) {
            targetUserIds.add(extraUserId);
        }
        if (roleCodes != null && !roleCodes.isEmpty()) {
            List<Long> roleIds = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                            .in(SysRole::getRoleCode, roleCodes))
                    .stream()
                    .map(SysRole::getId)
                    .toList();
            if (!roleIds.isEmpty()) {
                userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                                .in(SysUser::getRoleId, roleIds)
                                .eq(SysUser::getStatus, 1))
                        .stream()
                        .map(SysUser::getId)
                        .forEach(targetUserIds::add);
            }
        }
        targetUserIds.forEach(userId -> send(userId, title, content, bizType, bizId));
    }
}
