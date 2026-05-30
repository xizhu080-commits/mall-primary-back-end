package com.mall.demo.module.payment.service;

import cn.hutool.json.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mall.demo.module.payment.entity.Payment;
import com.mall.demo.module.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompensationService {

    private final PaymentMapper paymentMapper;
    private final AlipayClient alipayClient;
    private final PaymentServiceImpl paymentService;

    /**
     * 主动查询支付宝订单状态并补偿更新
     */
    @Transactional(rollbackFor = Exception.class)
    public void compensatePayment(String paymentId) {
        log.info("【补偿任务】开始主动查询支付单: {}", paymentId);

        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            log.warn("【补偿任务】支付单不存在: {}", paymentId);
            return;
        }
        if (payment.getStatus() == 1) {
            log.info("【补偿任务】支付单已成功，无需补偿: {}", paymentId);
            return;
        }
        if (payment.getStatus() == 2) {
            log.info("【补偿任务】支付单已失败，无需补偿: {}", paymentId);
            return;
        }

        // 调用支付宝查询接口
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.set("out_trade_no", payment.getPaymentId());
        request.setBizContent(bizContent.toString());

        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess() && "TRADE_SUCCESS".equals(response.getTradeStatus())) {
                log.warn("【补偿任务】发现掉单！支付单 {} 在支付宝已成功，本地状态={}", paymentId, payment.getStatus());

                // 构造模拟回调参数（只传必要字段）
                Map<String, String> mockParams = new HashMap<>();
                mockParams.put("out_trade_no", payment.getPaymentId());
                mockParams.put("trade_no", response.getTradeNo());
                mockParams.put("trade_status", "TRADE_SUCCESS");

                // 注意：这里不再调用 processAlipayNotify，而是直接调用核心业务方法
                // 因为 processAlipayNotify 里面还有验签、幂等检查等，补偿时不需要再走一遍
                paymentService.processPaymentSuccess(payment, mockParams);

                log.info("【补偿任务】成功修复掉单: {}", paymentId);

            } else if ("TRADE_CLOSED".equals(response.getTradeStatus())) {
                // 支付超时关闭
                payment.setStatus(2);
                paymentMapper.updateById(payment);
                log.info("【补偿任务】支付单已关闭: {}", paymentId);
            } else {
                log.info("【补偿任务】支付单 {} 仍在等待支付, 支付宝状态={}", paymentId, response.getTradeStatus());
            }

        } catch (AlipayApiException e) {
            log.error("【补偿任务】查询支付宝异常, paymentId={}", paymentId, e);
        }
    }
}