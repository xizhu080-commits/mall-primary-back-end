package com.mall.demo.module.payment.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.payment.service.PaymentNotifyMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/payment/notify")
@RequiredArgsConstructor
@Slf4j
public class PaymentNotifyMessageController {

    private final PaymentNotifyMessageService paymentNotifyMessageService;

    @PostMapping("/merchant/ship")
    public RestResp<Void> notifyMerchantForShip(
            @RequestParam @NotBlank(message = "支付单ID不能为空") String paymentId,
            @RequestParam @NotBlank(message = "子订单ID不能为空") String suborderId,
            @RequestParam @NotBlank(message = "商户ID不能为空") String merchantId) {


            paymentNotifyMessageService.notifyMerchantForShip(paymentId, suborderId, merchantId);
            return RestResp.ok( "通知商户发货成功", null);

    }


    @GetMapping("/test-push")
    public RestResp<String> testPush(@RequestParam String userId) {
        log.info("收到测试推送请求，用户ID: {}", userId);
        paymentNotifyMessageService.testPushToUser(userId);
        return RestResp.ok("已发送测试消息，请检查前端控制台");
    }

}