package com.campus.material.modules.apply.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.common.BizException;
import com.campus.material.common.OrderStatus;
import com.campus.material.modules.apply.dto.ApplyCreateRequest;
import com.campus.material.modules.apply.entity.ApplyOrder;
import com.campus.material.modules.apply.entity.ApplyOrderItem;
import com.campus.material.modules.apply.mapper.ApplyOrderItemMapper;
import com.campus.material.modules.apply.mapper.ApplyOrderMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApplyService {

    private final ApplyOrderMapper applyOrderMapper;
    private final ApplyOrderItemMapper applyOrderItemMapper;
    private final OperationLogService operationLogService;

    public ApplyService(ApplyOrderMapper applyOrderMapper, ApplyOrderItemMapper applyOrderItemMapper,
                        OperationLogService operationLogService) {
        this.applyOrderMapper = applyOrderMapper;
        this.applyOrderItemMapper = applyOrderItemMapper;
        this.operationLogService = operationLogService;
    }

    public List<ApplyOrder> list() {
        return applyOrderMapper.selectList(new LambdaQueryWrapper<ApplyOrder>().orderByDesc(ApplyOrder::getId));
    }

    public List<ApplyOrderItem> items(Long orderId) {
        return applyOrderItemMapper.selectList(new LambdaQueryWrapper<ApplyOrderItem>().eq(ApplyOrderItem::getApplyOrderId, orderId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(ApplyCreateRequest request) {
        ApplyOrder order = new ApplyOrder();
        order.setDeptId(request.getDeptId());
        order.setApplicantId(AuthUtil.currentUserId());
        order.setUrgencyLevel(request.getUrgencyLevel() == null ? 0 : request.getUrgencyLevel());
        order.setStatus(OrderStatus.DRAFT);
        order.setReason(request.getReason());
        order.setScenario(request.getScenario());
        order.setFastTrack(0);
        applyOrderMapper.insert(order);

        for (ApplyCreateRequest.Item item : request.getItems()) {
            ApplyOrderItem orderItem = new ApplyOrderItem();
            orderItem.setApplyOrderId(order.getId());
            orderItem.setMaterialId(item.getMaterialId());
            orderItem.setApplyQty(item.getApplyQty());
            orderItem.setActualQty(0);
            applyOrderItemMapper.insert(orderItem);
        }
        operationLogService.log(AuthUtil.currentUserId(), "APPLY", "CREATE", "创建申领单:" + order.getId());
        return detail(order.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void submit(Long orderId) {
        ApplyOrder order = mustGet(orderId);
        if (!OrderStatus.DRAFT.equals(order.getStatus())) {
            throw new BizException("当前状态不允许提交");
        }
        if (order.getUrgencyLevel() != null && order.getUrgencyLevel() >= 2) {
            order.setFastTrack(1);
            order.setStatus(OrderStatus.APPROVED);
            order.setApproverId(AuthUtil.currentUserId());
            order.setApproveRemark("紧急申领自动审批通过");
            order.setApproveTime(LocalDateTime.now());
        } else {
            order.setStatus(OrderStatus.SUBMITTED);
        }
        applyOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "APPLY", "SUBMIT", "提交申领单:" + orderId);
    }

    public void approve(Long orderId, String remark) {
        ApplyOrder order = mustGet(orderId);
        if (!OrderStatus.SUBMITTED.equals(order.getStatus())) {
            throw new BizException("当前状态不允许审批");
        }
        order.setStatus(OrderStatus.APPROVED);
        order.setApproverId(AuthUtil.currentUserId());
        order.setApproveRemark(remark);
        order.setApproveTime(LocalDateTime.now());
        applyOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "APPLY", "APPROVE", "审批通过申领单:" + orderId);
    }

    public void reject(Long orderId, String remark) {
        ApplyOrder order = mustGet(orderId);
        if (!OrderStatus.SUBMITTED.equals(order.getStatus())) {
            throw new BizException("当前状态不允许驳回");
        }
        order.setStatus(OrderStatus.REJECTED);
        order.setApproverId(AuthUtil.currentUserId());
        order.setApproveRemark(remark);
        order.setApproveTime(LocalDateTime.now());
        applyOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "APPLY", "REJECT", "驳回申领单:" + orderId);
    }

    public void receive(Long orderId) {
        ApplyOrder order = mustGet(orderId);
        if (!OrderStatus.OUTBOUND.equals(order.getStatus())) {
            throw new BizException("当前状态不允许签收");
        }
        order.setStatus(OrderStatus.RECEIVED);
        applyOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "APPLY", "RECEIVE", "签收申领单:" + orderId);
    }

    public Map<String, Object> detail(Long orderId) {
        ApplyOrder order = mustGet(orderId);
        List<ApplyOrderItem> items = items(orderId);
        Map<String, Object> map = new HashMap<>();
        map.put("order", order);
        map.put("items", items);
        return map;
    }

    private ApplyOrder mustGet(Long orderId) {
        ApplyOrder order = applyOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("申领单不存在");
        }
        return order;
    }
}
