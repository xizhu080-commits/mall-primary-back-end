package com.mall.demo.module.payment.service;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.demo.common.config.RabbitMQConfig;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.mapper.MessageRecordMapper;
import com.mall.demo.module.messageRecord.service.UserSessionService;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.module.payment.entity.Payment;
import com.mall.demo.module.payment.entity.PaymentNotifyFallback;
import com.mall.demo.module.payment.entity.PaymentNotifyMessage;
import com.mall.demo.module.payment.mapper.PaymentMapper;
import com.mall.demo.module.payment.mapper.PaymentNotifyFallbackMapper;
import com.mall.demo.module.payment.mapper.PaymentNotifyMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentNotifyMessageServiceImpl implements PaymentNotifyMessageService {
    private final SuborderMapper suborderMapper;
    private final PaymentNotifyMessageMapper paymentNotifyMessageMapper;
    private final PaymentNotifyFallbackMapper paymentNotifyFallbackMapper;
    private final MessageRecordMapper messageRecordMapper;
    private final SimpMessagingTemplate websocket;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentMapper paymentMapper;

    private final RedisService redisService;

    private final UserSessionService userSessionService;

    private static final String SEPARATOR = "::";








    /*
    * 通知商家发货
    * */
    @Override
    public void notifyMerchantForShip(String paymentId, String suborderId, String merchantId) {
        log.info("通知发货人发货，子订单ID：{}，支付单ID：{}", suborderId, paymentId);

        String paymentCacheKey = "payment:" + paymentId;
        Payment payment = redisService.get(
            paymentCacheKey,
            Payment.class,
            (key) -> {
                log.info("缓存未命中，从数据库查询支付单，paymentId: {}", paymentId);
                return paymentMapper.selectById(paymentId);
            },
            30
        );

        if (payment == null || payment.getStatus() != 1) {
            throw new RuntimeException("支付单不存在或请先支付");
        }

        String suborderCacheKey = "suborder:" + suborderId;
        Suborder suborder = redisService.get(
            suborderCacheKey,
            Suborder.class,
            (key) -> {
                log.info("缓存未命中，从数据库查询子订单，suborderId: {}", suborderId);
                return suborderMapper.selectById(suborderId);
            },
            30
        );

        if (suborder == null || suborder.getStatus() != 2) {
            throw new RuntimeException("子订单不存在或状态错误");
        }

        String targetShopId = suborder.getShopId();

        String content = "子订单：" + suborderId + "已支付，请尽快发货";
        String notifyId = IdUtil.getSnowflakeNextIdStr();

        PaymentNotifyMessage message = PaymentNotifyMessage.builder()
                .notifyId(notifyId)
                .orderId(payment.getOrderId())
                .paymentId(paymentId)
                .suborderId(suborderId)
                .targetUserId(targetShopId)
                .targetUserType("MERCHANT")
                .title("新的发货通知")
                .content(content)
                .createTime(LocalDateTime.now())
                .isRead(false)
                .build();

        saveToMessageTable(message);

        boolean wsSuccess = sendWebSocket(message);

        sendToMQ(message, RabbitMQConfig.PAYMENT_NOTIFY_EXCHANGE, RabbitMQConfig.PAYMENT_NOTIFY_ROUTING_KEY);

        storeToRedis(message);

        updatePushStatus(notifyId, wsSuccess);
    }



    /*
    * 通知用户--支付消息
    * */
    @Override
    public void notifyUserForPayment(String paymentId, String userId, String orderId, String suborderId_JSON) {
        log.info("通知用户 {} 支付成功，支付单ID：{}", userId, paymentId);

        String paymentCacheKey = "payment:" + paymentId;
        Payment payment = redisService.get(
            paymentCacheKey,
            Payment.class,
            (key) -> {
                log.info("缓存未命中，从数据库查询支付单，paymentId: {}", paymentId);
                return paymentMapper.selectById(paymentId);
            },
            30
        );

        if (payment == null || payment.getStatus() != 1) {
            throw new RuntimeException("支付单不存在或状态错误");
        }

        String content = "您的主订单：" + payment.getOrderId() + "已支付成功";

        String notifyId = IdUtil.getSnowflakeNextIdStr();
        PaymentNotifyMessage message = PaymentNotifyMessage.builder()
                .notifyId(notifyId)
                .paymentId(paymentId)
                .orderId(orderId)
                .suborderId(suborderId_JSON)
                .targetUserId(userId)
                .targetUserType("USER")
                .title("新的支付通知--支付成功!")
                .content(content)
                .createTime(LocalDateTime.now())
                .isRead(false)
                .build();

        saveToMessageTable(message);

        boolean wsSuccess = sendWebSocket(message);

        if (!wsSuccess) {
            sendToMQ(message, RabbitMQConfig.PAYMENT_NOTIFY_EXCHANGE, RabbitMQConfig.PAYMENT_NOTIFY_ROUTING_KEY);
        }

        storeToRedis(message);

        updatePushStatus(notifyId, wsSuccess);
    }


















    // 添加测试方法，可以手动调用
    @Override
    public void testPushToUser(String userId) {
        log.info("========== 测试推送 ==========");
        log.info("测试推送给用户: {}", userId);

        Map<String, Object> testMessage = new HashMap<>();
        testMessage.put("title", "测试支付成功通知");
        testMessage.put("orderId", "TEST_ORDER_001");
        testMessage.put("content", "halo哈喽!这是一条测试消息!");
        testMessage.put("timestamp", LocalDateTime.now().toString());

        try {
            websocket.convertAndSendToUser(userId, "/queue/userPaymentNotify", testMessage);
            log.info("测试消息推送成功给用户: {}", userId);
        } catch (Exception e) {
            log.error("测试消息推送失败", e);
        }
    }





    /**
     * WebSocket 推送（实时）
     */
    /**
     * WebSocket 推送（实时）
     */

    // ... existing code ...
    /**
     * WebSocket 推送（实时）
     */
    private boolean sendWebSocket(PaymentNotifyMessage message) {
        try {
            // ✅ 统一使用 convertAndSendToUser，根据用户类型选择不同队列
            String destination = "MERCHANT".equals(message.getTargetUserType())
                    ? "/queue/merchantPaymentNotify"
                    : "/queue/userPaymentNotify";

            websocket.convertAndSendToUser(
                    message.getTargetUserId(),
                    destination,
                    message
            );

            log.info("WebSocket 推送成功: userId={}, userType={}, 目标队列: {}",
                    message.getTargetUserId(), message.getTargetUserType(), destination);
            return true;
        } catch (Exception e) {
            log.error("WebSocket 推送失败: {}", e.getMessage(), e);
            return false;
        }
    }
// ... existing code ...



    /**
     * 发送到 RabbitMQ（持久化兜底）
     */
    private void sendToMQ(PaymentNotifyMessage message, String exchange, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("MQ 消息发送成功，exchange: {}", exchange);
        } catch (Exception e) {
            log.error("MQ 发送失败，需要记录到数据库兜底", e);
            // 终极兜底：存入数据库，定时任务扫描重试
            saveToFallbackTable(message);
        }
    }

    /**
     * 存 Redis（商家/用户轮询用）
     */
    private void storeToRedis(PaymentNotifyMessage message) {
        try {
            // 1. 修正占位符：%d 改为 %s，防止 String 类型转换异常
            String key = String.format("notify:%s:%s:list",
                    message.getTargetUserType().toLowerCase(),
                    message.getTargetUserId());

            // 2. 序列化处理
            // 如果你的 RedisTemplate 配置了 Jackson2JsonRedisSerializer，直接 push 对象即可
            // 如果没配置，建议使用已经写好的 json 字符串
            String json = objectMapper.writeValueAsString(message);

            // 3. 使用管道或事务确保原子性（可选），这里简单处理：入队并裁剪
            // LeftPush: 新消息放在最前面
            redisTemplate.opsForList().leftPush(key, json);

            // 4. 重点：只保留最近的 100 条通知，防止 Redis 内存溢出
            redisTemplate.opsForList().trim(key, 0, 99);

            // 5. 设置过期时间（7天）
            redisTemplate.expire(key, 7, TimeUnit.DAYS);

            log.info("通知已存入 Redis, Key: {}", key);
        } catch (Exception e) {
            log.error("Redis 存储失败，Key: {}", message.getTargetUserId(), e);
        }
    }



    /**
     * 存入消息表（MySQL - payment_notify_message  - message_record, user_session）
     */
      /**
     * 存入消息表（MySQL - payment_notify_message  - message_record, user_session）
     */
    private void saveToMessageTable(PaymentNotifyMessage message) {
        try {
            // 设置推送状态为待推送
            message.setPushStatus(0);
            message.setRetryCount(0);

            int result = paymentNotifyMessageMapper.insert(message);

            String targetUserId = message.getTargetUserId();
            if (targetUserId == null || targetUserId.isEmpty()) {
                throw new IllegalArgumentException("目标用户ID不能为空");
            }

            String fromId = "SYSTEM_PAYMENT";
            String toId = targetUserId;

            List<String> ids = java.util.Arrays.asList(fromId, toId);
            ids.sort(String::compareTo);
            String sessionId = ids.get(0) + SEPARATOR + ids.get(1);

            String systemName = "支付通知";

            // 保存到消息记录表（message_record）
            MessageRecord messageRecordEntity = new MessageRecord();
            messageRecordEntity.setMessageRecordId(message.getNotifyId());
            messageRecordEntity.setMessagePublisherType("PAYMENT");
            messageRecordEntity.setMessagePublisherId(message.getPaymentId());
            messageRecordEntity.setTargetUserId(targetUserId);
            messageRecordEntity.setTargetUserType(message.getTargetUserType());
            messageRecordEntity.setContent(message.getContent());
            messageRecordEntity.setPushStatus(1);
            messageRecordEntity.setRetryCount(0);
            messageRecordEntity.setIsRead(message.getIsRead());
            messageRecordEntity.setReadTime(message.getReadTime());
            messageRecordEntity.setCreateTime(message.getCreateTime());
            messageRecordEntity.setUpdateTime(message.getCreateTime());
            messageRecordEntity.setSessionId(sessionId);
            messageRecordMapper.insert(messageRecordEntity);

            // 只为真实用户创建会话，不创建系统侧的会话记录
            userSessionService.updateOrCreate(targetUserId, sessionId, fromId, messageRecordEntity, systemName, null, false);

            if (result > 0) {
                log.info("消息已存入 payment_notify_message 表，notifyId: {}", message.getNotifyId());
            }
        } catch (Exception e) {
            log.error("存入 message 表失败", e);
            throw new RuntimeException("保存消息失败", e);
        }
    }


    /**
     * 更新推送状态
     */
    private void updatePushStatus(String notifyId, boolean success) {
        try {
            // 使用 LambdaUpdateWrapper 根据 notifyId 更新，避开主键 id 为 0 的问题
            paymentNotifyMessageMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PaymentNotifyMessage>()
                    .set(PaymentNotifyMessage::getPushStatus, success ? 1 : 2)
                    .set(PaymentNotifyMessage::getRetryCount, success ? 0 : 1)
                    .eq(PaymentNotifyMessage::getNotifyId, notifyId)
            );
            log.info("更新推送状态成功, notifyId: {}, status: {}", notifyId, success ? 1 : 2);
        } catch (Exception e) {
            log.error("更新推送状态失败", e);
        }
    }

        /* 终极兜底：存 fallback 表（MySQL - payment_notify_fallback）
            * 定时任务会扫描这张表，重试发送到 MQ
     */
    private void saveToFallbackTable(PaymentNotifyMessage message) {
        try {
            // 将整个消息对象转为 JSON 字符串存储
            String messageJson = objectMapper.writeValueAsString(message);

            PaymentNotifyFallback fallback = new PaymentNotifyFallback();
            fallback.setMessageJson(messageJson);
            fallback.setRetryCount(0);
            fallback.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
            // 1分钟后重试
            fallback.setStatus(0);
            // 0-待重试
            fallback.setCreateTime(LocalDateTime.now());

            int result = paymentNotifyFallbackMapper.insert(fallback);
            if (result > 0) {
                log.info("消息已进入兜底表 payment_notify_fallback，notifyId: {}", message.getNotifyId());
            }
        } catch (Exception e) {
            log.error("存入兜底表失败", e);
        }
    }


}
