package com.campus.material.modules.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.material.common.BizException;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.modules.apply.service.ApplyService;
import com.campus.material.modules.delivery.entity.DeliveryTask;
import com.campus.material.modules.delivery.mapper.DeliveryTaskMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class DeliveryService {

    public static final String PENDING = "PENDING";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String SIGNED = "SIGNED";

    private final DeliveryTaskMapper deliveryTaskMapper;
    private final ApplyService applyService;
    private final OperationLogService operationLogService;

    public DeliveryService(DeliveryTaskMapper deliveryTaskMapper, ApplyService applyService, OperationLogService operationLogService) {
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.applyService = applyService;
        this.operationLogService = operationLogService;
    }

    public PageResult<DeliveryTask> list(PageQuery pageQuery, String status) {
        Page<DeliveryTask> page = deliveryTaskMapper.selectPage(
                new Page<>(pageQuery.getPage(), pageQuery.getSize()),
                new LambdaQueryWrapper<DeliveryTask>()
                        .eq(status != null && !status.isBlank(), DeliveryTask::getStatus, status)
                        .orderByDesc(DeliveryTask::getId)
        );
        return PageResult.from(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public DeliveryTask create(DeliveryTask task) {
        validateCreate(task);
        task.setStatus(PENDING);
        deliveryTaskMapper.insert(task);
        operationLogService.log(AuthUtil.currentUserId(), "DELIVERY", "CREATE", "配送任务:" + task.getId());
        return deliveryTaskMapper.selectById(task.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, Long dispatcherId) {
        if (dispatcherId == null) {
            throw new BizException(400, "配送人员不能为空");
        }
        DeliveryTask task = mustGet(id);
        if (!PENDING.equals(task.getStatus()) && !ASSIGNED.equals(task.getStatus())) {
            throw new BizException("当前状态不允许派单");
        }
        task.setDispatcherId(dispatcherId);
        task.setStatus(ASSIGNED);
        deliveryTaskMapper.updateById(task);
        operationLogService.log(AuthUtil.currentUserId(), "DELIVERY", "ASSIGN", "派单:" + id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        DeliveryTask task = mustGet(id);
        if (!ASSIGNED.equals(task.getStatus())) {
            throw new BizException("当前状态不允许开始配送");
        }
        task.setStatus(IN_TRANSIT);
        deliveryTaskMapper.updateById(task);
        operationLogService.log(AuthUtil.currentUserId(), "DELIVERY", "START", "开始配送:" + id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void sign(Long id) {
        DeliveryTask task = mustGet(id);
        if (!Set.of(ASSIGNED, IN_TRANSIT).contains(task.getStatus())) {
            throw new BizException("当前状态不允许签收");
        }
        if (task.getApplyOrderId() != null) {
            applyService.receive(task.getApplyOrderId());
        }
        task.setStatus(SIGNED);
        task.setSignedAt(LocalDateTime.now());
        deliveryTaskMapper.updateById(task);
        operationLogService.log(AuthUtil.currentUserId(), "DELIVERY", "SIGN", "签收:" + id);
    }

    private DeliveryTask mustGet(Long id) {
        DeliveryTask task = deliveryTaskMapper.selectById(id);
        if (task == null) {
            throw new BizException(404, "配送任务不存在");
        }
        return task;
    }

    private void validateCreate(DeliveryTask task) {
        if (task.getApplyOrderId() == null && task.getStockOutId() == null) {
            throw new BizException(400, "申领单或出库单至少填写一个");
        }
        if (task.getReceiverName() == null || task.getReceiverName().isBlank()) {
            throw new BizException(400, "收货人不能为空");
        }
        if (task.getDeliveryAddress() == null || task.getDeliveryAddress().isBlank()) {
            throw new BizException(400, "配送地址不能为空");
        }
        task.setReceiverName(task.getReceiverName().trim());
        task.setDeliveryAddress(task.getDeliveryAddress().trim());
        if (task.getReceiverPhone() != null) {
            task.setReceiverPhone(task.getReceiverPhone().trim());
        }
    }
}
