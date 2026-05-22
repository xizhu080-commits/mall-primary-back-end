package com.mall.demo.module.Refund.service;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.demo.common.config.RabbitMQConfig;
import com.mall.demo.module.Refund.entity.Refund;
import com.mall.demo.module.Refund.entity.RefundNotifyFallback;
import com.mall.demo.module.Refund.entity.RefundNotifyMessage;
import com.mall.demo.module.Refund.mapper.RefundNotifyFallbackMapper;
import com.mall.demo.module.Refund.mapper.RefundNotifyMessageMapper;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.mapper.MessageRecordMapper;
import com.mall.demo.module.messageRecord.mapper.UserSessionMapper;
import com.mall.demo.module.messageRecord.service.UserSessionService;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.user.entity.User;
import com.mall.demo.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundNotifyServiceImpl implements RefundNotifyService {

    private final SimpMessagingTemplate websocket;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MessageRecordMapper messageRecordMapper;

    // 新增 Mapper
    private final RefundNotifyMessageMapper refundNotifyMessageMapper;
    private final RefundNotifyFallbackMapper refundNotifyFallbackMapper;



    private final UserSessionService userSessionService;

    private static final String SEPARATOR = "::";


    /**
     * 用户申请退款 → 通知商家
     */
    @Override
    @Transactional
    public void notifyMerchantForApply(Refund refund, Suborder suborder, String content,String title) {
        log.info("通知商家退货退款申请，退货退款单ID：{}，商家ID：{}", refund.getRefundId(), refund.getMerchantId());

        // 生成雪花ID
        String notifyId = IdUtil.getSnowflakeNextIdStr();



        RefundNotifyMessage message = RefundNotifyMessage.builder()
                .notifyId(notifyId)
                .refundId(refund.getRefundId())
                .targetUserId(refund.getMerchantId())
                .targetUserType("MERCHANT")
                .title(title)
                .content(content)
                .action("APPLY")
                .pushStatus(0)
                // 0-待推送
                .retryCount(0)
                .createTime(LocalDateTime.now())
                .isRead(false)
                .build();

        // 1. 【重要】先存入 message 表（永久保存)
        saveToMessageTable(message);

        // 2. WebSocket 实时推送
        boolean wsSuccess = sendWebSocket(message);

        // 3. RabbitMQ 持久化兜底
        sendToMQ(message, RabbitMQConfig.REFUND_EXCHANGE, RabbitMQConfig.REFUND_ROUTING_KEY);

        // 4. Redis 待处理列表（商家轮询展示红点）
        storeToRedis(message);

        // 5. 更新推送状态
        updatePushStatus(notifyId, wsSuccess);
    }

    /**
     * 商家处理退款 → 通知用户
     */
    @Override
    @Transactional
    public void notifyUserForHandle(Refund refund, Boolean agreed, String rejectReason, String content,String title) {
        log.info("通知用户退款结果，退款单ID：{}，用户ID：{}，是否同意：{}",
                refund.getRefundId(), refund.getUserId(), agreed);

        String action;
        String notifyId = IdUtil.getSnowflakeNextIdStr();

        if (agreed) {
            action = "AGREE";
        } else {
            action = "REJECT";
        }

        RefundNotifyMessage message = RefundNotifyMessage.builder()
                .notifyId(notifyId)
                .refundId(refund.getRefundId())
                .targetUserId(refund.getUserId())
                .targetUserType("USER")
                .title(title)
                .content(content)
                .action(action)
                .pushStatus(0)
                // 0-待推送
                .retryCount(0)
                .createTime(LocalDateTime.now())
                .isRead(false)
                .build();

        // 1. 存入 message 表
        saveToMessageTable(message);

        // 2. WebSocket 推送
        boolean wsSuccess = sendWebSocket(message);

        // 3. RabbitMQ 兜底
        sendToMQ(message, RabbitMQConfig.HANDLE_RESULT_EXCHANGE, RabbitMQConfig.HANDLE_RESULT_ROUTING_KEY);

        // 4. Redis 存储
        storeToRedis(message);

        // 5. 更新推送状态
        updatePushStatus(notifyId, wsSuccess);
    }









    /**
     * WebSocket 推送（实时）
     */
    private boolean sendWebSocket(RefundNotifyMessage message) {
        try {
            // ✅ 修正：使用 convertAndSendToUser 并利用 Principal 路由
            String destination = "MERCHANT".equals(message.getTargetUserType())
                    ? "/queue/merchantRefundNotify"
                    : "/queue/userRefundNotify";

            websocket.convertAndSendToUser(
                    message.getTargetUserId(),
                    destination,
                    message
            );


            log.info("WebSocket 推送成功: userId={}, userType={}, 前端WebSocket队列: /user{}",
                    message.getTargetUserId(), message.getTargetUserType(), destination);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 推送失败，用户可能不在线: {}", e.getMessage());
            return false;
        }
    }




    /**
     * 存入消息表（MySQL - refund_notify_message 表    message_record 表保存）
     */
    private void saveToMessageTable(RefundNotifyMessage message) {
        try {
            int result = refundNotifyMessageMapper.insert(message);


            String fromId  = "[SYSTEM·REFUND·0001]";

            String toId = "[" + message.getTargetUserId() + "]";

            if (toId == null) {
                throw new IllegalArgumentException("目标用户ID不能为空");
            }

            List<String> ids = java.util.Arrays.asList(fromId, toId);
            ids.sort(String::compareTo);
            String sessionId = ids.get(0) + SEPARATOR + ids.get(1);
              String systemName = "退款通知";




            // 保存到消息记录表（message_record）
            MessageRecord messageRecordEntity = new MessageRecord();
            messageRecordEntity.setMessageRecordId(message.getNotifyId());
            messageRecordEntity.setMessagePublisherType("REFUND");
            messageRecordEntity.setMessagePublisherId(message.getRefundId());
            messageRecordEntity.setTargetUserId(message.getTargetUserId());
            messageRecordEntity.setTargetUserType(message.getTargetUserType());
            messageRecordEntity.setContent(message.getContent());
            messageRecordEntity.setIsRead(message.getIsRead());
            messageRecordEntity.setReadTime(message.getReadTime());
            messageRecordEntity.setCreateTime(message.getCreateTime());
            messageRecordEntity.setUpdateTime(message.getCreateTime());
            messageRecordEntity.setSessionId(sessionId);
            messageRecordMapper.insert(messageRecordEntity);






            //存入会话表:
            //接收方ID,     发送方ID  消息内容     会话ID     对方昵称
            userSessionService.upsertSession(message.getTargetUserId(), fromId, messageRecordEntity, sessionId, systemName,null);






            if (result > 0) {
                log.info("退款消息已存入 refund_notify_message 表，notifyId: {}", message.getNotifyId());
            }
        } catch (Exception e) {
            log.error("存入 refund_notify_message 表失败", e);
            throw new RuntimeException("保存退款消息失败", e);
        }
    }

    /**
     * 更新推送状态
     */
// ... existing code ...
    /**
     * 更新推送状态
     */
    private void updatePushStatus(String notifyId, boolean success) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RefundNotifyMessage> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(RefundNotifyMessage::getNotifyId, notifyId);

            RefundNotifyMessage updateEntity = new RefundNotifyMessage();
            updateEntity.setPushStatus(success ? 1 : 2);
            if (!success) {
                updateEntity.setRetryCount(1);
            }
            refundNotifyMessageMapper.update(updateEntity, wrapper);
        } catch (Exception e) {
            log.error("更新退款消息推送状态失败", e);
        }
    }
// ... existing code ...


    /**
     * 发送到 RabbitMQ（持久化兜底）
     */
    private void sendToMQ(RefundNotifyMessage message, String exchange, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("MQ 消息发送成功，exchange: {}", exchange);
        } catch (Exception e) {
            log.error("MQ 发送失败，需要记录到数据库兜底", e);
            // 终极兜底：存入 fallback 表，等待定时任务重试
            saveToFallbackTable(message);
        }
    }

    /**
     * 存 Redis（商家/用户轮询用）
     */
    private void storeToRedis(RefundNotifyMessage message) {
        try {
            String key = String.format("notify:%s:%s:list",
                    message.getTargetUserType().toLowerCase(),
                    message.getTargetUserId());

            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(key, json);
            redisTemplate.opsForList().trim(key, 0, 99);
            // 只保留最近100条
            redisTemplate.expire(key, 7, TimeUnit.DAYS);

            log.info("退款通知已存入 Redis, Key: {}", key);
        } catch (Exception e) {
            log.error("Redis 存储失败", e);
        }
    }

    /**
     * 终极兜底：存 fallback 表（MySQL - refund_notify_fallback）
     * 定时任务会扫描这张表，重试发送到 MQ
     */
    private void saveToFallbackTable(RefundNotifyMessage message) {
        try {
            // 将整个消息对象转为 JSON 字符串存储
            String messageJson = objectMapper.writeValueAsString(message);

            RefundNotifyFallback fallback = new RefundNotifyFallback();
            fallback.setMessageJson(messageJson);
            fallback.setRetryCount(0);
            fallback.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
            // 1分钟后重试
            fallback.setStatus(0);
            // 0-待重试
            fallback.setCreateTime(LocalDateTime.now());

            int result = refundNotifyFallbackMapper.insert(fallback);
            if (result > 0) {
                log.info("退款消息已进入兜底表 refund_notify_fallback，notifyId: {}", message.getNotifyId());
            }
        } catch (Exception e) {
            log.error("存入兜底表失败", e);
        }
    }
}