package com.mall.demo.module.Refund.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.TradeRecord;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.Refund.dto.req.ApplyRefundReqDto;
import com.mall.demo.module.Refund.dto.req.HandleRefundReqDto;
import com.mall.demo.module.Refund.dto.resp.ApplyRefundRespDto;
import com.mall.demo.module.Refund.dto.resp.HandleRefundRespDto;
import com.mall.demo.module.Refund.entity.Refund;
import com.mall.demo.module.Refund.mapper.RefundMapper;
import com.mall.demo.module.TransactionRecord.entity.TransactionRecord;
import com.mall.demo.module.TransactionRecord.mapper.TransactionRecordMapper;
import com.mall.demo.module.logistic.entity.Logistic;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.SuborderMapper;

import com.mall.demo.module.order.service.StockService;
import com.mall.demo.module.payment.entity.Payment;
import com.mall.demo.module.payment.mapper.PaymentMapper;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final RefundMapper refundMapper;
    private final ShopMapper shopMapper;
    private final SuborderMapper suborderMapper;
    private final PaymentMapper paymentMapper;
     private final StockService stockService;
     private final RefundNotifyService refundNotifyService;
     private final TransactionRecordMapper transactionRecordMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplyRefundRespDto applyRefund(ApplyRefundReqDto dto) {
        //用户申请退款
        String userId = SecurityUtils.getId();

        // 1. 获取子订单并校验
        Suborder suborder = suborderMapper.selectById(dto.getSuborderId());
        if (suborder == null) {
            throw new BizException(ErrorCodeEnum.ORDER_NOT_EXIST.getCode(), ErrorCodeEnum.ORDER_NOT_EXIST.getMessage());
        }
        if (!suborder.getUserId().equals(userId)) {
            throw new BizException(ErrorCodeEnum.NO_PERMISSION.getCode(), ErrorCodeEnum.NO_PERMISSION.getMessage());
        }
        //'订单状态: 1->待付款；2->待发货；3->已发货；4->已完成；5->退款中; 6->退款成功; 0->已关闭',

        // 2. 状态校验拦截
        int status = suborder.getStatus();
        if (status == 4) throw new BizException(ErrorCodeEnum.ORDER_CLOSED.getCode(), ErrorCodeEnum.ORDER_CLOSED.getMessage());
        if (status == 0) throw new BizException(ErrorCodeEnum.ORDER_NOT_PAY.getCode(), ErrorCodeEnum.ORDER_NOT_PAY.getMessage());
        if (status == 5) throw new BizException(500, "订单正在退款中，请勿重复申请");

        if (status == 3) {
            LocalDateTime superTime = suborder.getUpdateTime().plusDays(7);
            if (superTime.isBefore(LocalDateTime.now())) {
                throw new BizException(ErrorCodeEnum.ORDER_NOT_SUPPORT_REFUND.getCode(), "已超过7天无理由退款期");
            }

        }

        // 3. 校验店铺是否为当前用户所属店铺
        String shopId = suborder.getShopId();
        Shop shop = shopMapper.selectById(shopId);
        String merchantId = shop.getMerchantId();


        LambdaQueryWrapper<Payment> paymentQueryWrapper = new LambdaQueryWrapper<>();
        paymentQueryWrapper.eq(Payment::getOrderId, suborder.getOrderId());
        Payment payment = paymentMapper.selectOne(paymentQueryWrapper);

        if (payment == null) {
            throw new BizException(500, "订单不存在或未支付");
        }





        // 3. 构建并插入退款单 (提取出来的公共逻辑)
        Refund refund = new Refund();
        refund.setSuborderId(suborder.getSuborderId());
        refund.setOrderId(suborder.getOrderId());
        refund.setUserId(userId);
        refund.setMerchantId(merchantId);
        refund.setPaymentId(payment.getPaymentId());


        refund.setRefundReason(dto.getRefundReason());
        refund.setRefundAmount(suborder.getPayAmount());
        refund.setStatus(0);
        refundMapper.insert(refund);
  //  子订单      '订单状态: 1->待付款；2->待发货；3->已发货；4->待签收；5->签收成功; 6->退款中; 7->退款成功; 0->已关闭',
        // 4. 更新子订单状态为 "退款中(5)"
        suborder.setStatus(6);
        suborderMapper.updateById(suborder);

        // 5. 特殊处理：如果是“待发货(1)”状态，无需商家审核，直接发起“秒退”
        if (status == 1) {
            log.info("待发货订单，触发自动退款流程，子订单号：{}", suborder.getSuborderId());
            executeAlipayRefund(refund, suborder);
            String title = "新的退货退款通知--用户申请退款----您无需处理!";
            String content = buildLogisticNotifyJson(suborder, refund, title);

            refundNotifyService.notifyMerchantForApply(refund,suborder,content,title);
        }

        String title = "新的退货退款通知--退货退款申请!";
        String content = buildLogisticNotifyJson(suborder, refund, title);


        // 5. 发送退款通知-->商家
        refundNotifyService.notifyMerchantForApply(refund,suborder,content,title);


        // 6. 构建返回对象
        ApplyRefundRespDto respDto = new ApplyRefundRespDto();
        respDto.setRefundId(refund.getRefundId());
        respDto.setUserId(userId);
        respDto.setSuborderId(suborder.getSuborderId());
        respDto.setOrderId(suborder.getOrderId());
        // 返回父单号
        respDto.setRefundAmount(suborder.getPayAmount());
        respDto.setRefundReason(dto.getRefundReason());
        respDto.setApplyRefundTime(refund.getCreateTime());
        return respDto;
    }


    /**
     * 商家处理退款审核 (同意或拒绝)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HandleRefundRespDto handleRefund(HandleRefundReqDto dto) {

        String merchantId = SecurityUtils.getId();

        Refund refund = refundMapper.selectById(dto.getRefundId());
        if (refund == null || refund.getStatus() != 0) {
            throw new BizException(500, "退款单不存在或已被处理");
        }

        Suborder suborder = suborderMapper.selectById(refund.getSuborderId());

        if (dto.getIsAgree()) {
            // 商家同意，执行实际打款

            executeAlipayRefund(refund, suborder);
            String title = "新的退货退款通知--退货退款成功!";
            String content = buildLogisticNotifyJson(suborder, refund, title);

            // 发送通知给用户-->退款成功
            refundNotifyService.notifyUserForHandle(refund, dto.getIsAgree(), null,content,title);
        } else {
            // 商家拒绝退款
            refund.setStatus(2);

            refund.setUpdateTime(LocalDateTime.now());
            // 2: 退款失败/已拒绝
            refund.setRejectReason(dto.getRejectReason());
            refundMapper.updateById(refund);

            // 恢复子订单原来的状态 (需要你在表里设计一个 original_status 或者写死恢复为待发货/待收货)
            suborder.setStatus(4);
            suborder.setUpdateTime(LocalDateTime.now());
            // 粗略示例：假设恢复为待收货
            suborderMapper.updateById(suborder);


            String title = "新的退货退款通知--退货退款失败--" + dto.getRejectReason();
            String content = buildLogisticNotifyJson(suborder, refund, title);

            refundNotifyService.notifyUserForHandle(refund, dto.getIsAgree(), dto.getRejectReason(),content,title);
        }

        // 6. 构建返回对象
        HandleRefundRespDto respDto = new HandleRefundRespDto();
        respDto.setMerchantId(merchantId);
        respDto.setSuborderId(suborder.getSuborderId());
        respDto.setOrderId(suborder.getOrderId());
        respDto.setRefundId(refund.getRefundId());
        respDto.setStatus(refund.getStatus());
        respDto.setRejectReason(refund.getRejectReason());
        respDto.setHandleTime(refund.getUpdateTime());
        return respDto;
    }


    /**
     * 内部方法：执行支付宝真正退款及相关资源回滚
     */
    private final AlipayClient alipayClient;

    private void executeAlipayRefund(Refund refund, Suborder suborder) {


        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            JSONObject bizContent = new JSONObject();
            // ✅ 必须是支付单号
            bizContent.set("out_trade_no", refund.getPaymentId());

            //退款金额
            bizContent.set("refund_amount", refund.getRefundAmount().toString());

            // ✅ 部分退款唯一标识: 退款单号
          bizContent.set("out_request_no", refund.getRefundId());

            request.setBizContent(bizContent.toString());

            AlipayTradeRefundResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {

                log.info("支付宝退款成功: {}", refund.getRefundId());

                // 1. 更新退款单
                refund.setStatus(1);
                refund.setUpdateTime(LocalDateTime.now());
                log.error("准备更新退款单");
                refundMapper.updateById(refund);

                // 2. 更新子订单
                suborder.setStatus(6);
                // 已退款
                log.error("准备更新子订单");
                suborderMapper.updateById(suborder);
                log.error("准备回滚库存");
                // 3. 回滚库存（修复数量问题）
                stockService.addStock(suborder.getSkuId(),1);


                // 4. 交易记录
                LambdaQueryWrapper<TransactionRecord> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TransactionRecord::getOrderId, suborder.getOrderId())
                        .eq(TransactionRecord::getSuborderId, suborder.getSuborderId());
                TransactionRecord transactionRecord = transactionRecordMapper.selectOne(queryWrapper);
                transactionRecord.setTransactionStatus(2);
                transactionRecord.setUpdateTime(LocalDateTime.now());
                transactionRecord.setRemark("退款成功---交易关闭");
                transactionRecordMapper.updateById(transactionRecord);


            } else {
                log.error("退款失败: {}", response.getSubMsg());
                throw new BizException("支付宝退款失败：" + response.getSubMsg());
            }

        } catch (Exception e) {
            log.error("退款异常", e);
            throw new BizException("退款异常");
        }
    }













    /**
     * ✅ 抽取公共方法：构建物流通知 JSON 内容
     *
     * @param suborder 子订单信息
     * @param refund 退款单信息
     * @param title 通知标题
     * @return JSON 格式的通知内容
     */
    private String buildLogisticNotifyJson(Suborder suborder, Refund refund, String title) {
        JSONObject contentJson = JSONUtil.createObj();

        // 消息类型和标题
        contentJson.set("type", "REFUND_NOTIFY");
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

        // 退款单信息
        JSONObject refundJson = JSONUtil.createObj();
        refundJson.set("refundId", refund.getRefundId());
        refundJson.set("refundAmount", refund.getRefundAmount());
        refundJson.set("status", refund.getStatus());
        refundJson.set("rejectReason", refund.getRejectReason());
        contentJson.set("refund", refundJson);

        // 收货人信息
        JSONObject receiverJson = JSONUtil.createObj();
        receiverJson.set("receiverName", suborder.getReceiverName());
        receiverJson.set("address", suborder.getAddress());
        contentJson.set("receiver", receiverJson);

        return contentJson.toString();
    }





}