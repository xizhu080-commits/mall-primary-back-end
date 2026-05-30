package com.mall.demo.module.payment.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.module.payment.entity.Payment;
import com.mall.demo.module.payment.mapper.PaymentMapper;
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
    private final PaymentMapper paymentMapper;
    private final SuborderMapper suborderMapper;


    /*
    * 支付系统消息 ---> 通知商户发货
    * */
    @PostMapping("/merchant/ship")
    public RestResp<Void> notifyMerchantForShip(
            @RequestParam @NotBlank(message = "支付单ID不能为空") String paymentId,
            @RequestParam @NotBlank(message = "子订单ID不能为空") String suborderId) {

        // ✅ 从数据库查询获取 shopId
        Suborder suborder = suborderMapper.selectById(suborderId);
        if (suborder == null) {
            return RestResp.fail(400, "子订单不存在");
        }

        String targetShopId = suborder.getShopId();
        if (targetShopId == null || targetShopId.isEmpty()) {
            return RestResp.fail(400, "子订单未关联店铺");
        }

        paymentNotifyMessageService.notifyMerchantForShip(paymentId, suborderId, targetShopId);
        return RestResp.ok("通知商户发货成功", null);
    }


    /*
    * 支付系统消息 ---> 通知用户支付成功
    * */
    @PostMapping("/user/payment")
    public RestResp<Void> notifyUserForPayment(
            @RequestParam @NotBlank(message = "支付单ID不能为空") String paymentId,
            @RequestParam @NotBlank(message = "订单ID不能为空") String orderId,
            @RequestParam(required = false) String suborderIdJson) {

        // ✅ 从数据库查询获取 userId
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            return RestResp.fail(400, "支付单不存在");
        }

        String userId = payment.getUserId();
        paymentNotifyMessageService.notifyUserForPayment(paymentId, userId, orderId, suborderIdJson);
        return RestResp.ok("通知用户支付成功", null);
    }



    /*
    * 测试推送（开发环境使用）
    * */
    @GetMapping("/test-push")
    public RestResp<String> testPush(@RequestParam String userId) {
        log.info("收到测试推送请求，用户ID: {}", userId);
        paymentNotifyMessageService.testPushToUser(userId);
        return RestResp.ok("已发送测试消息，请检查前端控制台");
    }

}
