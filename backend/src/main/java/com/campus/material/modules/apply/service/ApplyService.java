package com.campus.material.modules.apply.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.material.common.BizException;
import com.campus.material.common.OrderStatus;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.monitoring.BusinessMetrics;
import com.campus.material.modules.apply.dto.ApplyCreateRequest;
import com.campus.material.modules.apply.entity.ApplyOrder;
import com.campus.material.modules.apply.entity.ApplyOrderItem;
import com.campus.material.modules.apply.mapper.ApplyOrderItemMapper;
import com.campus.material.modules.apply.mapper.ApplyOrderMapper;
import com.campus.material.modules.inventory.service.InventoryService;
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
    private final InventoryService inventoryService;
    private final OperationLogService operationLogService;
    private final BusinessMetrics businessMetrics;

    public ApplyService(ApplyOrderMapper applyOrderMapper, ApplyOrderItemMapper applyOrderItemMapper,
                        InventoryService inventoryService, OperationLogService operationLogService, BusinessMetrics businessMetrics) {
        this.applyOrderMapper = applyOrderMapper;
        this.applyOrderItemMapper = applyOrderItemMapper;
        this.inventoryService = inventoryService;
        this.operationLogService = operationLogService;
        this.businessMetrics = businessMetrics;
    }

    public PageResult<ApplyOrder> list(PageQuery pageQuery) {
        Page<ApplyOrder> page = applyOrderMapper.selectPage(
                new Page<>(pageQuery.getPage(), pageQuery.getSize()),
                new LambdaQueryWrapper<ApplyOrder>().orderByDesc(ApplyOrder::getId)
        );
        return PageResult.from(page);
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
        List<ApplyOrderItem> applyItems = items(orderId);
        Long reservedWarehouseId = inventoryService.reserveForApplyOrder(applyItems);
        /*
         * 申领单在提交阶段完成第一次状态跃迁。
         * 同时在事务内锁定库存，避免多个申请并发提交时出现超卖。
         * 紧急等级达到阈值时直接走快速通道，服务端在同一事务内补齐审批人、审批意见与审批时间，
         * 避免前端或后续异步任务再拼装半完成状态。
         */
        order.setReservedWarehouseId(reservedWarehouseId);
        if (order.getUrgencyLevel() != null && order.getUrgencyLevel() >= 2) {
            order.setFastTrack(1);
            order.setStatus(OrderStatus.APPROVED);
            order.setApproverId(AuthUtil.currentUserId());
            order.setApproveRemark("紧急申领自动审批通过");
            order.setApproveTime(LocalDateTime.now());
        } else {
            order.setStatus(OrderStatus.SUBMITTED);
        }
        ensureUpdated(applyOrderMapper.updateById(order), "申领单状态已被其他操作修改，请刷新后重试");
        operationLogService.log(AuthUtil.currentUserId(), "APPLY", "SUBMIT", "提交申领单:" + orderId);
        businessMetrics.recordApplySubmit();
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long orderId, String remark) {
        ApplyOrder order = mustGet(orderId);
        if (!OrderStatus.SUBMITTED.equals(order.getStatus())) {
            throw new BizException("当前状态不允许审批");
        }
        // 审批只允许从 SUBMITTED 进入 APPROVED，避免重复审批覆盖原有审批信息。
        order.setStatus(OrderStatus.APPROVED);
        order.setApproverId(AuthUtil.currentUserId());
        order.setApproveRemark(remark);
        order.setApproveTime(LocalDateTime.now());
        ensureUpdated(applyOrderMapper.updateById(order), "申领单状态已被其他操作修改，请刷新后重试");
        operationLogService.log(AuthUtil.currentUserId(), "APPLY", "APPROVE", "审批通过申领单:" + orderId);
        businessMetrics.recordApplyApprove();
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long orderId, String remark) {
        ApplyOrder order = mustGet(orderId);
        if (!OrderStatus.SUBMITTED.equals(order.getStatus())) {
            throw new BizException("当前状态不允许驳回");
        }
        inventoryService.releaseApplyReservation(order.getReservedWarehouseId(), items(orderId));
        LocalDateTime approveTime = LocalDateTime.now();
        // 驳回和审批共享同一前置状态，确保单据不会在已审批或已出库后被反向改写。
        ensureUpdated(applyOrderMapper.update(null, new LambdaUpdateWrapper<ApplyOrder>()
                        .eq(ApplyOrder::getId, orderId)
                        .eq(ApplyOrder::getVersion, order.getVersion())
                        .eq(ApplyOrder::getStatus, OrderStatus.SUBMITTED)
                        .set(ApplyOrder::getStatus, OrderStatus.REJECTED)
                        .set(ApplyOrder::getReservedWarehouseId, null)
                        .set(ApplyOrder::getApproverId, AuthUtil.currentUserId())
                        .set(ApplyOrder::getApproveRemark, remark)
                        .set(ApplyOrder::getApproveTime, approveTime)
                        .set(ApplyOrder::getVersion, order.getVersion() + 1)),
                "申领单状态已被其他操作修改，请刷新后重试");
        operationLogService.log(AuthUtil.currentUserId(), "APPLY", "REJECT", "驳回申领单:" + orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void receive(Long orderId) {
        ApplyOrder order = mustGet(orderId);
        if (!OrderStatus.OUTBOUND.equals(order.getStatus())) {
            throw new BizException("当前状态不允许签收");
        }
        inventoryService.releaseApplyReservation(order.getReservedWarehouseId(), items(orderId));
        ensureUpdated(applyOrderMapper.update(null, new LambdaUpdateWrapper<ApplyOrder>()
                        .eq(ApplyOrder::getId, orderId)
                        .eq(ApplyOrder::getVersion, order.getVersion())
                        .eq(ApplyOrder::getStatus, OrderStatus.OUTBOUND)
                        .set(ApplyOrder::getStatus, OrderStatus.RECEIVED)
                        .set(ApplyOrder::getReservedWarehouseId, null)
                        .set(ApplyOrder::getVersion, order.getVersion() + 1)),
                "申领单状态已被其他操作修改，请刷新后重试");
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

    private void ensureUpdated(int updatedRows, String message) {
        if (updatedRows != 1) {
            throw new BizException(message);
        }
    }
}
