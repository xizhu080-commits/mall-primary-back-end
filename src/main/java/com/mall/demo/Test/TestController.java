package com.mall.demo.Test;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.Refund.entity.Refund;
import com.mall.demo.module.Refund.service.RefundNotifyService;
import com.mall.demo.module.logistic.entity.Logistic;
import com.mall.demo.module.logistic.service.LogisticNotifyService;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.service.MessageRecordService;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.payment.service.PaymentNotifyMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/test/websocket")
@RequiredArgsConstructor
@Tag(name = "WebSocket 推送测试", description = "用于测试各类消息的 WebSocket 推送功能")
public class TestController {

    private final PaymentNotifyMessageService paymentNotifyMessageService;
    private final RefundNotifyService refundNotifyService;
    private final LogisticNotifyService logisticNotifyService;
    private final MessageRecordService messageRecordService;

    // ==================== 支付消息测试 ====================

    @GetMapping("/payment/user")
    @Operation(summary = "测试支付成功通知（用户端）", description = "模拟用户支付成功后，向用户推送支付成功通知")
    public RestResp<String> testPaymentToUser(
            @Parameter(description = "用户ID") @RequestParam(defaultValue = "2049420760040607746") String userId,
            @Parameter(description = "支付单ID") @RequestParam(defaultValue = "TEST_PAYMENT_001") String paymentId,
            @Parameter(description = "订单ID") @RequestParam(defaultValue = "TEST_ORDER_001") String orderId,
            @Parameter(description = "子订单JSON") @RequestParam(defaultValue = "[\"SUBORDER_001\"]") String suborderIdJson) {

        log.info("========== 测试支付通知推送（用户端） ==========");
        log.info("用户ID: {}, 支付单ID: {}, 订单ID: {}", userId, paymentId, orderId);

        try {
            paymentNotifyMessageService.testPushToUser(userId);
            return RestResp.ok("✅ 支付通知推送成功");
        } catch (Exception e) {
            log.error("❌ 支付通知推送失败", e);
            return RestResp.fail(400, "支付通知推送失败: " + e.getMessage());
        }
    }

    @GetMapping("/payment/merchant")
    @Operation(summary = "测试发货通知（商家端）", description = "模拟用户支付成功后，向商家推送发货通知")
    public RestResp<String> testPaymentToMerchant(
            @Parameter(description = "商家ID") @RequestParam(defaultValue = "2049420760040607746") String merchantId,
            @Parameter(description = "支付单ID") @RequestParam(defaultValue = "TEST_PAYMENT_002") String paymentId,
            @Parameter(description = "子订单ID") @RequestParam(defaultValue = "TEST_SUBORDER_001") String suborderId
    ) {

        log.info("========== 测试发货通知推送（商家端） ==========");
        log.info("商家ID: {}, 支付单ID: {}, 子订单ID: {}", merchantId, paymentId, suborderId);

        try {
            paymentNotifyMessageService.testPushToUser(merchantId);
            return RestResp.ok("✅ 发货通知推送成功");
        } catch (Exception e) {
            log.error("❌ 发货通知推送失败", e);
            return RestResp.fail(400, "发货通知推送失败: " + e.getMessage());
        }
    }

    // ==================== 退款消息测试 ====================

    @GetMapping("/refund/apply")
    @Operation(summary = "测试退款申请通知（商家端）", description = "模拟用户申请退款后，向商家推送退款申请通知")
    public RestResp<String> testRefundApply(
            @Parameter(description = "商家ID") @RequestParam(defaultValue = "2049420760040607746") String merchantId,
            @Parameter(description = "用户ID") @RequestParam(defaultValue = "12345671") String userId,
            @Parameter(description = "退款金额") @RequestParam(defaultValue = "99.00") BigDecimal refundAmount,
            @Parameter(description = "退款原因") @RequestParam(defaultValue = "商品质量问题") String refundReason) {

        log.info("========== 测试退款申请通知（商家端） ==========");

        String refundId = "TEST_REFUND_" + System.currentTimeMillis();
        String suborderId = "TEST_SUBORDER_001";

        Refund refund = new Refund();
        refund.setRefundId(refundId);
        refund.setMerchantId(merchantId);
        refund.setUserId(userId);
        refund.setSuborderId(suborderId);
        refund.setRefundAmount(refundAmount);
        refund.setRefundReason(refundReason);
        refund.setCreateTime(LocalDateTime.now());

        Suborder suborder = new Suborder();
        suborder.setSuborderId(suborderId);
        suborder.setProductName("测试商品");

        String content = String.format("用户申请退款，退款金额：¥%s，原因：%s", refundAmount, refundReason);
        String title = "新的退款申请";

        try {
            refundNotifyService.notifyMerchantForApply(refund, suborder, content, title);
            return RestResp.ok("✅ 退款申请通知推送成功 - 退款单ID: " + refundId);
        } catch (Exception e) {
            log.error("❌ 退款申请通知推送失败", e);
            return RestResp.fail(400, "退款申请通知推送失败: " + e.getMessage());
        }
    }

    @GetMapping("/refund/agree")
    @Operation(summary = "测试退款同意通知（用户端）", description = "模拟商家同意退款后，向用户推送退款通过通知")
    public RestResp<String> testRefundAgree(
            @Parameter(description = "用户ID") @RequestParam(defaultValue = "2049420760040607746") String userId,
            @Parameter(description = "退款金额") @RequestParam(defaultValue = "99.00") BigDecimal refundAmount) {

        log.info("========== 测试退款同意通知（用户端） ==========");

        String refundId = "TEST_REFUND_AGREE_" + System.currentTimeMillis();

        Refund refund = new Refund();
        refund.setRefundId(refundId);
        refund.setUserId(userId);
        refund.setMerchantId("1");
        refund.setRefundAmount(refundAmount);
        refund.setCreateTime(LocalDateTime.now());

        String content = String.format("您的退款申请已通过审核，退款金额 ¥%s 将在3-7个工作日内原路返回", refundAmount);
        String title = "退款申请已通过";

        try {
            refundNotifyService.notifyUserForHandle(refund, true, null, content, title);
            return RestResp.ok("✅ 退款同意通知推送成功 - 退款单ID: " + refundId);
        } catch (Exception e) {
            log.error("❌ 退款同意通知推送失败", e);
            return RestResp.fail(400, "退款同意通知推送失败: " + e.getMessage());
        }
    }

    @GetMapping("/refund/reject")
    @Operation(summary = "测试退款拒绝通知（用户端）", description = "模拟商家拒绝退款后，向用户推送退款拒绝通知")
    public RestResp<String> testRefundReject(
            @Parameter(description = "用户ID") @RequestParam(defaultValue = "2049420760040607746") String userId,
            @Parameter(description = "退款金额") @RequestParam(defaultValue = "99.00") BigDecimal refundAmount,
            @Parameter(description = "拒绝原因") @RequestParam(defaultValue = "商品已使用，不符合退款条件") String rejectReason) {

        log.info("========== 测试退款拒绝通知（用户端） ==========");

        String refundId = "TEST_REFUND_REJECT_" + System.currentTimeMillis();

        Refund refund = new Refund();
        refund.setRefundId(refundId);
        refund.setUserId(userId);
        refund.setMerchantId("1");
        refund.setRefundAmount(refundAmount);
        refund.setCreateTime(LocalDateTime.now());

        String content = String.format("您的退款申请已被拒绝，原因：%s", rejectReason);
        String title = "退款申请被拒绝";

        try {
            refundNotifyService.notifyUserForHandle(refund, false, rejectReason, content, title);
            return RestResp.ok("✅ 退款拒绝通知推送成功 - 退款单ID: " + refundId);
        } catch (Exception e) {
            log.error("❌ 退款拒绝通知推送失败", e);
            return RestResp.fail(400, "退款拒绝通知推送失败: " + e.getMessage());
        }
    }

    // ==================== 物流消息测试 ====================

    @GetMapping("/logistic/sign")
    @Operation(summary = "测试物流签收通知（用户端）", description = "模拟包裹到达后，向用户推送签收通知")
    public RestResp<String> testLogisticSign(
            @Parameter(description = "用户ID") @RequestParam(defaultValue = "2049420760040607746") String userId,
            @Parameter(description = "快递单号") @RequestParam(defaultValue = "SF1234567890") String trackingNumber) {

        log.info("========== 测试物流签收通知（用户端） ==========");

        String logisticId = "TEST_LOGISTIC_" + System.currentTimeMillis();
        String content = String.format("您的包裹已到达配送站，请及时签收。快递单号：%s", trackingNumber);

        try {
            logisticNotifyService.notifyUserForSign(logisticId, userId, content);
            return RestResp.ok("✅ 物流签收通知推送成功 - 物流单ID: " + logisticId);
        } catch (Exception e) {
            log.error("❌ 物流签收通知推送失败", e);
            return RestResp.fail(400, "物流签收通知推送失败: " + e.getMessage());
        }
    }

    @GetMapping("/logistic/merchant/agree")
    @Operation(summary = "测试用户签收确认通知（商家端-同意）", description = "模拟用户确认签收后，向商家推送通知")
    public RestResp<String> testLogisticMerchantAgree(
            @Parameter(description = "商家ID") @RequestParam(defaultValue = "2049420760040607746") String merchantId,
            @Parameter(description = "快递单号") @RequestParam(defaultValue = "SF1234567890") String trackingNumber) {

        log.info("========== 测试用户签收确认通知（商家端） ==========");

        String logisticId = "TEST_LOGISTIC_" + System.currentTimeMillis();

        Logistic logistic = new Logistic();
        logistic.setLogisticId(logisticId);
        logistic.setShipperId(merchantId);
        logistic.setConsigneeId("1");
        logistic.setLogisticCompanyId(trackingNumber);
        logistic.setStatus(1);
        logistic.setCreateTime(LocalDateTime.now());

        String content = String.format("用户已确认签收包裹，快递单号：%s", trackingNumber);

        try {
            logisticNotifyService.notifyMerchantForHandle(logistic, true, null, content);
            return RestResp.ok("✅ 用户签收确认通知推送成功 - 物流单ID: " + logisticId);
        } catch (Exception e) {
            log.error("❌ 用户签收确认通知推送失败", e);
            return RestResp.fail(400, "用户签收确认通知推送失败: " + e.getMessage());
        }
    }

    @GetMapping("/logistic/merchant/reject")
    @Operation(summary = "测试用户拒签通知（商家端-拒绝）", description = "模拟用户拒签包裹后，向商家推送通知")
    public RestResp<String> testLogisticMerchantReject(
            @Parameter(description = "商家ID") @RequestParam(defaultValue = "2049420760040607746") String merchantId,
            @Parameter(description = "快递单号") @RequestParam(defaultValue = "SF1234567891") String trackingNumber,
            @Parameter(description = "拒签原因") @RequestParam(defaultValue = "包裹外包装破损") String rejectReason) {

        log.info("========== 测试用户拒签通知（商家端） ==========");

        String logisticId = "TEST_LOGISTIC_" + System.currentTimeMillis();

        Logistic logistic = new Logistic();
        logistic.setLogisticId(logisticId);
        logistic.setShipperId(merchantId);
        logistic.setConsigneeId("1");
        logistic.setLogisticCompanyId(trackingNumber);
        logistic.setStatus(1);
        logistic.setCreateTime(LocalDateTime.now());

        String content = String.format("用户拒绝签收包裹，原因：%s。快递单号：%s", rejectReason, trackingNumber);

        try {
            logisticNotifyService.notifyMerchantForHandle(logistic, false, rejectReason, content);
            return RestResp.ok("✅ 用户拒签通知推送成功 - 物流单ID: " + logisticId);
        } catch (Exception e) {
            log.error("❌ 用户拒签通知推送失败", e);
            return RestResp.fail(400, "用户拒签通知推送失败: " + e.getMessage());
        }
    }

    @GetMapping("/logistic/shipment")
    @Operation(summary = "测试商家发货通知（用户端）", description = "模拟商家发货后，向用户推送发货通知")
    public RestResp<String> testLogisticShipment(
            @Parameter(description = "用户ID") @RequestParam(defaultValue = "2049420760040607746") String userId,
            @Parameter(description = "快递单号") @RequestParam(defaultValue = "SF1234567892") String trackingNumber) {

        log.info("========== 测试商家发货通知（用户端） ==========");

        String logisticId = "TEST_LOGISTIC_" + System.currentTimeMillis();

        Logistic logistic = new Logistic();
        logistic.setLogisticId(logisticId);
        logistic.setShipperId("1");
        logistic.setConsigneeId(userId);
        logistic.setLogisticCompanyId(trackingNumber);
        logistic.setStatus(0);
        logistic.setCreateTime(LocalDateTime.now());

        String content = String.format("商家已发货，快递单号：%s，预计3天内送达", trackingNumber);

        try {
            logisticNotifyService.notifyUserForHandle(logistic, userId, content);
            return RestResp.ok("✅ 商家发货通知推送成功 - 物流单ID: " + logisticId);
        } catch (Exception e) {
            log.error("❌ 商家发货通知推送失败", e);
            return RestResp.fail(400, "商家发货通知推送失败: " + e.getMessage());
        }
    }

    // ==================== 客服消息测试 ====================

    @GetMapping("/customer/to-user")
    @Operation(summary = "测试客服消息推送（用户端）", description = "模拟客服向用户发送消息")
    public RestResp<String> testCustomerMessageToUser(
            @Parameter(description = "用户ID") @RequestParam(defaultValue = "2049420760040607746") String userId,
            @Parameter(description = "消息内容") @RequestParam(defaultValue = "您好，欢迎咨询！请问有什么可以帮助您的？") String content) {

        log.info("========== 测试客服消息推送（用户端） ==========");

        String messageId = "TEST_MESSAGE_" + System.currentTimeMillis();

        MessageRecord messageRecord = new MessageRecord();
        messageRecord.setMessageRecordId(messageId);
        messageRecord.setMessagePublisherType("CUSTOMER");
        messageRecord.setMessagePublisherId("CS_001");
        messageRecord.setTargetUserId(userId);
        messageRecord.setTargetUserType("USER");
        messageRecord.setContent(content);
        messageRecord.setIsRead(false);
        messageRecord.setCreateTime(LocalDateTime.now());
        messageRecord.setUpdateTime(LocalDateTime.now());

        try {
            messageRecordService.sendWeChatMessageTest(messageRecord);
            return RestResp.ok("✅ 客服消息推送成功 - 消息ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 客服消息推送失败", e);
            return RestResp.fail(400, "客服消息推送失败: " + e.getMessage());
        }
    }

    @GetMapping("/customer/to-merchant")
    @Operation(summary = "测试客服消息推送（商家端）", description = "模拟客服向商家发送消息")
    public RestResp<String> testCustomerMessageToMerchant(
            @Parameter(description = "商家ID") @RequestParam(defaultValue = "2049420760040607746") String merchantId,
            @Parameter(description = "消息内容") @RequestParam(defaultValue = "平台客服通知：您的店铺有新的订单需要处理，请及时查看。") String content) {

        log.info("========== 测试客服消息推送（商家端） ==========");

        String messageId = "TEST_MESSAGE_" + System.currentTimeMillis();

        MessageRecord messageRecord = new MessageRecord();
        messageRecord.setMessageRecordId(messageId);
        messageRecord.setMessagePublisherType("CUSTOMER");
        messageRecord.setMessagePublisherId("CS_001");
        messageRecord.setTargetUserId(merchantId);
        messageRecord.setTargetUserType("MERCHANT");
        messageRecord.setContent(content);
        messageRecord.setIsRead(false);
        messageRecord.setCreateTime(LocalDateTime.now());
        messageRecord.setUpdateTime(LocalDateTime.now());

        try {
            messageRecordService.sendWeChatMessageTest(messageRecord);
            return RestResp.ok("✅ 客服消息推送成功 - 消息ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 客服消息推送失败", e);
            return RestResp.fail(400, "客服消息推送失败: " + e.getMessage());
        }
    }

    // ==================== 批量测试 ====================

    @GetMapping("/batch/all")
    @Operation(summary = "批量测试所有类型消息", description = "一次性测试所有类型的消息推送")
    public RestResp<String> testAllTypes(
            @Parameter(description = "用户ID") @RequestParam(defaultValue = "2049420760040607746") String userId,
            @Parameter(description = "商家ID") @RequestParam(defaultValue = "2049420760040607746") String merchantId) {

        log.info("========== 开始批量测试所有类型消息 ==========");

        StringBuilder result = new StringBuilder();
        int successCount = 0;
        int failCount = 0;

        // 1. 测试支付通知
        try {
            paymentNotifyMessageService.notifyUserForPayment(
                "BATCH_PAYMENT_001", userId, "BATCH_ORDER_001", "[\"BATCH_SUB_001\"]"
            );
            result.append("✅ 支付通知成功\n");
            successCount++;
        } catch (Exception e) {
            result.append("❌ 支付通知失败: ").append(e.getMessage()).append("\n");
            failCount++;
        }

        // 2. 测试发货通知
        try {
            paymentNotifyMessageService.notifyMerchantForShip("BATCH_PAYMENT_002", "BATCH_SUB_002", merchantId);
            result.append("✅ 发货通知成功\n");
            successCount++;
        } catch (Exception e) {
            result.append("❌ 发货通知失败: ").append(e.getMessage()).append("\n");
            failCount++;
        }

        // 3. 测试退款申请
        try {
            Refund refund = new Refund();
            refund.setRefundId("BATCH_REFUND_001");
            refund.setMerchantId(merchantId);
            refund.setUserId(userId);
            refund.setRefundAmount(new BigDecimal("99.00"));
            refund.setCreateTime(LocalDateTime.now());

            Suborder suborder = new Suborder();
            suborder.setSuborderId("BATCH_SUB_003");

            refundNotifyService.notifyMerchantForApply(
                refund, suborder, "批量测试退款申请", "批量测试退款"
            );
            result.append("✅ 退款申请成功\n");
            successCount++;
        } catch (Exception e) {
            result.append("❌ 退款申请失败: ").append(e.getMessage()).append("\n");
            failCount++;
        }

        // 4. 测试物流通知
        try {
            logisticNotifyService.notifyUserForSign(
                "BATCH_LOGISTIC_001", userId, "批量测试物流签收"
            );
            result.append("✅ 物流通知成功\n");
            successCount++;
        } catch (Exception e) {
            result.append("❌ 物流通知失败: ").append(e.getMessage()).append("\n");
            failCount++;
        }

        // 5. 测试客服消息
        try {
            MessageRecord messageRecord = new MessageRecord();
            messageRecord.setMessageRecordId("BATCH_MESSAGE_001");
            messageRecord.setMessagePublisherType("CUSTOMER_SERVICE");
            messageRecord.setMessagePublisherId("CS_001");
            messageRecord.setTargetUserId(userId);
            messageRecord.setTargetUserType("USER");
            messageRecord.setContent("批量测试客服消息");
            messageRecord.setIsRead(false);
            messageRecord.setCreateTime(LocalDateTime.now());
            messageRecord.setUpdateTime(LocalDateTime.now());

            messageRecordService.sendWeChatMessageTest(messageRecord);
            result.append("✅ 客服消息成功\n");
            successCount++;
        } catch (Exception e) {
            result.append("❌ 客服消息失败: ").append(e.getMessage()).append("\n");
            failCount++;
        }

        String summary = String.format("\n========== 测试完成 ==========\n成功: %d, 失败: %d\n%s",
            successCount, failCount, result.toString());

        log.info(summary);

        if (failCount == 0) {
            return RestResp.ok(summary);
        } else {
            return RestResp.fail(400, summary);
        }
    }

    // ==================== 请求/响应 DTO ====================

    @Data
    public static class PaymentTestRequest {
        private String userId;
        private String merchantId;
        private String paymentId;
        private String orderId;
        private String suborderId;
    }

    @Data
    public static class RefundTestRequest {
        private String userId;
        private String merchantId;
        private BigDecimal refundAmount;
        private String refundReason;
    }

    @Data
    public static class LogisticTestRequest {
        private String userId;
        private String merchantId;
        private String trackingNumber;
        private String rejectReason;
    }

    @Data
    public static class CustomerMessageRequest {
        private String targetId;
        private String targetType; // USER or MERCHANT
        private String content;
    }
}
