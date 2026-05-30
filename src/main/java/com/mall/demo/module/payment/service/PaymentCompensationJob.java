package com.mall.demo.module.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.module.payment.entity.Payment;
import com.mall.demo.module.payment.mapper.PaymentMapper;
import com.mall.demo.module.payment.service.PaymentCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompensationJob {

    private final PaymentMapper paymentMapper;
    private final PaymentCompensationService compensationService;

    /**
     * 每5分钟执行一次，扫描10分钟前创建但未支付的订单
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    @Transactional(readOnly = true)
    public void scanTimeoutPayments() {
        log.info("【补偿定时任务】开始扫描超时未支付的订单...");

        LocalDateTime timeout = LocalDateTime.now().minusMinutes(10);

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getStatus, 0)
                .lt(Payment::getCreateTime, timeout)
                .orderByAsc(Payment::getCreateTime)
                .last("LIMIT 100");

        List<Payment> pendingPayments = paymentMapper.selectList(wrapper);

        log.info("【补偿定时任务】共扫描到 {} 条待补偿的支付单", pendingPayments.size());

        for (Payment payment : pendingPayments) {
            try {
                compensationService.compensatePayment(payment.getPaymentId());
            } catch (Exception e) {
                log.error("【补偿定时任务】处理支付单 {} 失败", payment.getPaymentId(), e);
            }
        }

        log.info("【补偿定时任务】扫描完成");
    }
}