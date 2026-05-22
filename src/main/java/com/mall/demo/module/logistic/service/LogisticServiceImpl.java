package com.mall.demo.module.logistic.service;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.Refund.dto.req.ApplyRefundReqDto;
import com.mall.demo.module.Refund.dto.req.HandleRefundReqDto;
import com.mall.demo.module.Refund.entity.Refund;
import com.mall.demo.module.Refund.mapper.RefundMapper;
import com.mall.demo.module.Refund.service.RefundService;
import com.mall.demo.module.logistic.dto.req.CreateLogisticReqDto;
import com.mall.demo.module.logistic.entity.Logistic;
import com.mall.demo.module.logistic.mapper.LogisticMapper;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.mq.producer.LogisticProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogisticServiceImpl implements LogisticService {

    private final SuborderMapper suborderMapper;
    private final LogisticMapper logisticMapper;
    private final RefundMapper refundMapper;
    private final LogisticNotifyService logisticNotifyService;
    private final LogisticProducer logisticProducer;
    private final RefundService refundService;

    // 定义状态常量，增强可读性
    private static final int SUBORDER_WAIT_SHIP = 2;   // 待发货
    private static final int SUBORDER_WAIT_RECEIVE = 3; // 待收货
    private static final int SUBORDER_COMPLETED = 4;    // 已完成

    private static final int LOGISTIC_WAIT_SIGN = 1;    // 待签收
    private static final int LOGISTIC_SIGNED = 2;       // 已签收
    private static final int LOGISTIC_REJECTED = 3;     // 已拒签

    /**
     * 创建物流单--卖家发货
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createLogistic(CreateLogisticReqDto createLogisticReqDto) {
        Suborder suborder = suborderMapper.selectByIdForUpdate(createLogisticReqDto.getSuborderId());
        if (suborder == null) {
            throw new IllegalArgumentException("子订单不存在");
        }
        if (suborder.getStatus() != SUBORDER_WAIT_SHIP) {
            throw new IllegalArgumentException("子订单状态不是待发货");
        }

        LocalDateTime now = LocalDateTime.now();
        int randomDays = 5 + new Random().nextInt(3);
        LocalDateTime signTime = now.plusDays(randomDays);

        Logistic logistic = new Logistic();
        logistic.setBuyerId(suborder.getUserId());
        logistic.setLogisticCompanyId(createLogisticReqDto.getLogisticCompanyId());
        logistic.setLogisticCompanyName(createLogisticReqDto.getLogisticCompanyName());

        // 发货信息
        logistic.setShipper(createLogisticReqDto.getShipper());
        logistic.setShipperId(createLogisticReqDto.getShipperId());
        logistic.setShipperAddress(createLogisticReqDto.getShipperAddress());
        logistic.setShipperTime(now);

        // 收货信息
        logistic.setConsignee(createLogisticReqDto.getConsignee());
        logistic.setConsigneeId(createLogisticReqDto.getConsigneeId());
        logistic.setConsigneeAddress(createLogisticReqDto.getConsigneeAddress());

        logistic.setDeliveryTime(now);
        logistic.setSignTime(signTime);
        logistic.setAutoSignTime(signTime.plusDays(7));
        logistic.setOrderId(suborder.getOrderId());
        logistic.setSuborderId(suborder.getSuborderId());
        logistic.setStatus(LOGISTIC_WAIT_SIGN);
        logistic.setCreateTime(now);
        logistic.setUpdateTime(now);

        logisticMapper.insert(logistic);
        log.info("物流单创建成功，logisticId: {}, suborderId: {}", logistic.getLogisticId(), suborder.getSuborderId());

        suborder.setLogisticCompanyName(logistic.getLogisticCompanyName());
        suborderMapper.updateById(suborder);

        // 更新子订单状态为：待收货
        updateSuborderStatus(suborder.getSuborderId(), SUBORDER_WAIT_RECEIVE);
        log.info("子订单状态已更新为待收货，suborderId: {}", suborder.getSuborderId());

        // ✅ 构建物流通知 JSON 内容
        String content = buildLogisticNotifyJson(suborder, logistic, "您的订单已发货");

        // 物流通知: 商家发货 ----> 通知用户
        logisticNotifyService.notifyUserForSign(logistic.getLogisticId(), suborder.getUserId(), content);
        log.info("已发送物流发货通知给用户，userId: {}", suborder.getUserId());

        // 发送死信队列：处理自动签收
        logisticProducer.sendSignMessage(logistic);
        log.info("已发送自动签收死信消息，logisticId: {}", logistic.getLogisticId());
    }

    /**
     * 物流通知: 物流到达 ----> 通知用户签收确认
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applySignLogistic(String suborderId, String logisticId) {
        Suborder suborder = suborderMapper.selectByIdForUpdate(suborderId);
        if (suborder == null) {
            throw new IllegalArgumentException("子订单不存在");
        }
        if (suborder.getStatus() == SUBORDER_COMPLETED) {
            throw new IllegalArgumentException("订单已完成");
        }

        Logistic logistic = logisticMapper.selectById(logisticId);
        if (logistic == null || logistic.getStatus() != LOGISTIC_WAIT_SIGN) {
            throw new IllegalArgumentException("物流单状态不正确");
        }

        // ✅ 构建物流通知 JSON 内容
        String content = buildLogisticNotifyJson(suborder, logistic, "您的订单已到达，待签收！");

        logisticNotifyService.notifyUserForSign(logisticId, suborder.getUserId(), content);
        log.info("已发送物流到达通知给用户，userId: {}, logisticId: {}", suborder.getUserId(), logisticId);
    }

    /**
     * 处理签收/拒签逻辑
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleSignLogistic(String logisticId, boolean isAgree, String rejectReason) {
        String currentUserId = SecurityUtils.getId();
        Logistic logistic = logisticMapper.selectById(logisticId);

        if (logistic == null || logistic.getStatus() != LOGISTIC_WAIT_SIGN) {
            throw new BizException("物流单不可操作或已处理");
        }

        if (isAgree) {
            // --- 场景：同意签收 ---
            handleAgreeSign(logistic, currentUserId);
        } else {
            // --- 场景：拒绝签收 ---
            handleRejectSign(logistic, currentUserId, rejectReason);
        }
    }

    /**
     * 私有方法：处理同意签收
     */
    private void handleAgreeSign(Logistic logistic, String userId) {
        // 情况 A：正向物流，买家签收
        if (logistic.getConsigneeId().equals(userId)) {
            logistic.setStatus(LOGISTIC_SIGNED);
            logistic.setConsigneeTime(LocalDateTime.now());
            logisticMapper.updateById(logistic);
            log.info("买家已签收，logisticId: {}", logistic.getLogisticId());

            // ✅ 获取子订单信息用于构建通知
            Suborder suborder = suborderMapper.selectById(logistic.getSuborderId());
            String content = buildLogisticNotifyJson(suborder, logistic, "您已签收订单");

            // 更新子订单为完成
            updateSuborderStatus(logistic.getSuborderId(), SUBORDER_COMPLETED);
            log.info("子订单状态已更新为已完成，suborderId: {}", logistic.getSuborderId());

            // 通知商家签收结果
            logisticNotifyService.notifyMerchantForHandle(logistic, true, null,content);
            log.info("已通知商家买家已签收，merchantId: {}", logistic.getShipperId());
        }
        // 情况 B：逆向物流，商家签收用户退回的货
        else if (logistic.getShipperId().equals(userId)) {
            logistic.setStatus(LOGISTIC_SIGNED);
            logistic.setUpdateTime(LocalDateTime.now());
            logisticMapper.updateById(logistic);
            log.info("商家已签收退货，logisticId: {}", logistic.getLogisticId());

            processAutoRefund(logistic, "退货包裹商家已签收，系统自动结算退款");
            log.info("商家[{}]已签收退货，触发自动退款", userId);

            // ✅ 通知用户退货已被商家签收
            Suborder suborder = suborderMapper.selectById(logistic.getSuborderId());
            String content = buildLogisticNotifyJson(suborder, logistic, "商家已签收您的退货");
            logisticNotifyService.notifyUserForSign(logistic.getLogisticId(), suborder.getUserId(), content);
            log.info("已通知用户商家已签收退货，userId: {}", suborder.getUserId());
        } else {
            throw new BizException("权限不足：您不是该物流单的合法接收者");
        }
    }

    /**
     * 私有方法：处理拒签
     */
    private void handleRejectSign(Logistic logistic, String userId, String rejectReason) {
        // 校验：只有收货人才有权拒签
        if (!logistic.getConsigneeId().equals(userId)) {
            throw new BizException("非法操作：非收货人无法操作拒签");
        }

        logistic.setStatus(LOGISTIC_REJECTED);
        logistic.setUpdateTime(LocalDateTime.now());
        logisticMapper.updateById(logistic);
        log.info("用户已拒签，logisticId: {}, reason: {}", logistic.getLogisticId(), rejectReason);

        // 拒签通常涉及正向流程中断，触发退款
        processAutoRefund(logistic, "用户拒签，包裹将原路退回，系统发起自动退款");

        Suborder suborder = suborderMapper.selectById(logistic.getSuborderId());


        String content = buildLogisticNotifyJson(suborder, logistic, "您已拒签订单");


        // 通知商家
        logisticNotifyService.notifyMerchantForHandle(logistic, false, rejectReason,content);
        log.info("用户[{}]拒签了包裹，已发起自动退款流程，拒签原因: {}", userId, rejectReason);
    }

    /**
     * 自动退款核心逻辑（含防重校验）
     */
    private void processAutoRefund(Logistic logistic, String reason) {
        log.info("开始自动退款流程, 子订单ID: {}", logistic.getSuborderId());

        // 1. 先获取子订单信息（用于判断状态）
        Suborder suborder = suborderMapper.selectById(logistic.getSuborderId());
        if (suborder == null) {
            log.error("自动退款失败：子订单 {} 不存在", logistic.getSuborderId());
            return;
        }

        // 2. 申请退款
        refundService.applyRefund(ApplyRefundReqDto.builder()
                .suborderId(logistic.getSuborderId())
                .orderId(logistic.getOrderId())
                .refundReason(reason)
                .build());

        // 3. 判断是否为"秒退"状态
        if (suborder.getStatus() == 1) {
            log.info("子订单 {} 为待发货状态，applyRefund 已完成自动秒退，无需进入审核打款流程", suborder.getSuborderId());
            return;
        }

        // 4. 非"秒退"状态，需要查询并手动触发 handleRefund
        Refund refund = refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getSuborderId, logistic.getSuborderId())
                .orderByDesc(Refund::getCreateTime)
                .last("LIMIT 1"));

        if (refund == null) {
            log.error("退款失败：未找到对应的退款记录！");
            return;
        }

        log.info("准备为退款单 {} 发起打款审核...", refund.getRefundId());
        refundService.handleRefund(HandleRefundReqDto.builder()
                .refundId(refund.getRefundId())
                .isAgree(true)
                .suborderId(logistic.getSuborderId())
                .orderId(logistic.getOrderId())
                .rejectReason(reason)
                .build());
        log.info("自动审核打款流程完成");
    }

    /**
     * 更新子订单状态
     */
    private void updateSuborderStatus(String suborderId, Integer status) {
        Suborder suborder = new Suborder();
        suborder.setSuborderId(suborderId);
        suborder.setStatus(status);
        suborderMapper.updateById(suborder);
    }

    /**
     * ✅ 抽取公共方法：构建物流通知 JSON 内容
     *
     * @param suborder 子订单信息
     * @param logistic 物流单信息
     * @param title 通知标题
     * @return JSON 格式的通知内容
     */
    private String buildLogisticNotifyJson(Suborder suborder, Logistic logistic, String title) {
        JSONObject contentJson = JSONUtil.createObj();

        // 消息类型和标题
        contentJson.set("type", "LOGISTIC_NOTIFY");
        contentJson.set("title", title);
        contentJson.set("suborderId", suborder.getSuborderId());
        contentJson.set("orderId", suborder.getOrderId());

        // 商品信息
        JSONObject productJson = JSONUtil.createObj();
        productJson.set("shopId", suborder.getShopId());
        productJson.set("shopName", suborder.getShopName());
        productJson.set("skuId", suborder.getSkuId());
        productJson.set("productName", suborder.getProductName());
        productJson.set("spec", suborder.getSpecData() != null ? suborder.getSpecData() : "无");
        productJson.set("price", suborder.getPrice());
        productJson.set("payAmount", suborder.getPayAmount());
        productJson.set("productUrl", suborder.getProductUrl());
        contentJson.set("product", productJson);

        // 物流信息
        JSONObject logisticsJson = JSONUtil.createObj();
        logisticsJson.set("logisticCompanyName", logistic.getLogisticCompanyName());
        logisticsJson.set("logisticId", logistic.getLogisticId());
        contentJson.set("logistics", logisticsJson);

        // 收货人信息
        JSONObject receiverJson = JSONUtil.createObj();
        receiverJson.set("receiverName", suborder.getReceiverName());
        receiverJson.set("address", suborder.getAddress());
        contentJson.set("receiver", receiverJson);

        return contentJson.toString();
    }



}
