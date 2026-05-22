package com.mall.demo.module.logistic.service;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.demo.common.config.RabbitMQConfig;
import com.mall.demo.module.logistic.entity.Logistic;
import com.mall.demo.module.logistic.entity.LogisticNotifyMessage;
import com.mall.demo.module.logistic.entity.LogisticNotifyMessageFallback;
import com.mall.demo.module.logistic.mapper.LogisticNotifyMessageFallbackMapper;
import com.mall.demo.module.logistic.mapper.LogisticNotifyMessageMapper;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.mapper.MessageRecordMapper;
import com.mall.demo.module.messageRecord.service.UserSessionService;
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
public class LogisticNotifyServiceImpl implements LogisticNotifyService {

    private final SimpMessagingTemplate websocket;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MessageRecordMapper messageRecordMapper;

    // 新增 Mapper
    private final LogisticNotifyMessageMapper logisticNotifyMessageMapper;
    private final LogisticNotifyMessageFallbackMapper logisticNotifyMessageFallbackMapper;


    private final UserSessionService userSessionService;

    private static final String SEPARATOR = "::";




    /**
     * 物流到达 ---> 通知用户          maybe签收/拒签
     */
    @Override
    @Transactional
    public void notifyUserForSign(String logisticId, String consigneeId, String content) {
        log.info("通知用户签收确认，物流单ID：{}，用户ID：{}", logisticId, consigneeId);

        String notifyId = IdUtil.getSnowflakeNextIdStr();

        LogisticNotifyMessage message = LogisticNotifyMessage.builder()
                .notifyId(notifyId)
                .logisticId(logisticId)
                .targetUserId(consigneeId)
                .targetUserType("USER")
                .title("新的包裹请签收确认")
                .content(content)
                .action("SIGN")
                .pushStatus(0)
                .retryCount(0)
                .createTime(LocalDateTime.now())
                .isRead(false)
                .build();

        saveToMessageTable(message);

        boolean wsSuccess = sendWebSocket(message);

        sendToMQ(message, RabbitMQConfig.LOGISTIC_NOTIFY_EXCHANGE,
                 RabbitMQConfig.LOGISTIC_NOTIFY_ROUTING_KEY);

        storeToRedis(message);

        updatePushStatus(notifyId, wsSuccess);
    }

    /**
     * 用户处理物流单 → 通知商家    maybe同意/拒签
     */
    @Override
    @Transactional
    public void notifyMerchantForHandle(Logistic logistic, Boolean agreed, String rejectReason,String content) {
        log.info("通知商家签收结果，物流单ID：{}，商家ID：{}，是否同意：{}",
                logistic.getLogisticId(), logistic.getShipperId(), agreed);

        String logisticId = logistic.getLogisticId();
        String title = agreed ? "签收已通过" : "签收被拒绝";

        String action = agreed ? "AGREE" : "REJECT";



        String notifyId = IdUtil.getSnowflakeNextIdStr();

        LogisticNotifyMessage message = LogisticNotifyMessage.builder()
                .notifyId(notifyId)
                .logisticId(logisticId)
                .targetUserId(logistic.getShipperId())
                .targetUserType("MERCHANT")
                .title(title)
                .content(content)
                .action(action)
                .pushStatus(0)
                .retryCount(0)
                .createTime(LocalDateTime.now())
                .isRead(false)
                .build();

        saveToMessageTable(message);

        boolean wsSuccess = sendWebSocket(message);

        sendToMQ(message, RabbitMQConfig.LOGISTIC_NOTIFY_EXCHANGE,
                 RabbitMQConfig.LOGISTIC_NOTIFY_ROUTING_KEY);

        storeToRedis(message);

        updatePushStatus(notifyId, wsSuccess);
    }




    /**
     * 商家处理物流单 → 通知用户    maybe发货
    */
    @Override
    @Transactional
    public void notifyUserForHandle(Logistic logistic,String consigneeId, String content) {
        log.info("通知用户，物流单ID：{}，用户ID：{}，内容：{}",
                logistic.getLogisticId(), consigneeId, content);

        String logisticId = logistic.getLogisticId();


        String action = "NOTICE";

        String notifyId = IdUtil.getSnowflakeNextIdStr();

        LogisticNotifyMessage message = LogisticNotifyMessage.builder()
                .notifyId(notifyId)
                .logisticId(logisticId)
                .targetUserId(consigneeId)
                .targetUserType("USER")
                .title("新的物流通知")
                .content(content)
                .action(action)
                .pushStatus(0)
                .retryCount(0)
                .createTime(LocalDateTime.now())
                .isRead(false)
                .build();

        saveToMessageTable(message);

        boolean wsSuccess = sendWebSocket(message);

        sendToMQ(message, RabbitMQConfig.LOGISTIC_NOTIFY_EXCHANGE,
                RabbitMQConfig.LOGISTIC_NOTIFY_ROUTING_KEY);

        storeToRedis(message);

        updatePushStatus(notifyId, wsSuccess);
    }




















    /**
     * WebSocket 推送（实时）
     */
    private boolean sendWebSocket(LogisticNotifyMessage message) {
        try {
            // ✅ 修正：使用 convertAndSendToUser 并利用 Principal 路由
            String destination = "MERCHANT".equals(message.getTargetUserType())
                    ? "/queue/merchantLogisticNotify"
                    : "/queue/userLogisticNotify";

            websocket.convertAndSendToUser(
                    message.getTargetUserId(),
                    destination,
                    message
            );

            log.info("WebSocket 推送成功: userId={}, userType={}, 目标队列: {}",
                    message.getTargetUserId(), message.getTargetUserType(), destination);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 推送失败，用户可能不在线: {}", e.getMessage());
            return false;
        }
    }



    /**
     * 存入消息表（MySQL - logistic_notify_message  - message_record, user_session）
     */
    private void saveToMessageTable(LogisticNotifyMessage message) {
        try {
            int result = logisticNotifyMessageMapper.insert(message);



            String fromId  = "[SYSTEM·LOGISTIC·0002]";

            String toId = "[" + message.getTargetUserId() + "]";

            if (toId == null) {
                throw new IllegalArgumentException("目标用户ID不能为空");
            }


            List<String> ids = java.util.Arrays.asList(fromId, toId);
            ids.sort(String::compareTo);
            String sessionId = ids.get(0) + SEPARATOR + ids.get(1);



            String systemName = "物流通知";




            // 保存到消息记录表（message_record）
            MessageRecord messageRecordEntity = new MessageRecord();
            messageRecordEntity.setMessageRecordId(message.getNotifyId());
            // ✅ 修正：物流通知应该标记为 "LOGISTIC"
            messageRecordEntity.setMessagePublisherType("LOGISTIC");
            messageRecordEntity.setMessagePublisherId(message.getLogisticId());
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
                log.info("物流消息已存入 logistic_notify_message 表，notifyId: {}", message.getNotifyId());
            }
        } catch (Exception e) {
            log.error("存入 logistic_notify_message 表失败", e);
            throw new RuntimeException("保存物流消息失败", e);
        }
    }



    /**
     * 存 Redis（商家/用户轮询用）
     */
    private void storeToRedis(LogisticNotifyMessage message) {
        try {
            // ✅ 修正：使用独立的 Redis key 前缀 "logistic"
            String key = String.format("logistic:%s:%s:list",
                    message.getTargetUserType().toLowerCase(),
                    message.getTargetUserId());

            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(key, json);
            redisTemplate.opsForList().trim(key, 0, 99);
            redisTemplate.expire(key, 7, TimeUnit.DAYS);

            log.info("物流通知已存入 Redis, Key: {}", key);
        } catch (Exception e) {
            log.error("Redis 存储失败，Key: {}", message.getTargetUserId(), e);
        }
    }



    /**
     * 更新推送状态
     */
    private void updatePushStatus(String notifyId, boolean success) {
        try {
            LogisticNotifyMessage message = LogisticNotifyMessage.builder()
                    .notifyId(notifyId)
                    .pushStatus(success ? 1 : 2)
                    .retryCount(success ? 0 : 1)
                    .build();
            logisticNotifyMessageMapper.updateById(message);
        } catch (Exception e) {
            log.error("更新物流消息推送状态失败", e);
        }
    }






    /**
     * 终极兜底：存 fallback 表（MySQL - logistic_notify_fallback）
     * 定时任务会扫描这张表，重试发送到 MQ
     */
    private void saveToFallbackTable(LogisticNotifyMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);

            LogisticNotifyMessageFallback fallback = new LogisticNotifyMessageFallback();
            fallback.setMessageJson(messageJson);
            fallback.setRetryCount(0);
            fallback.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
            fallback.setStatus(0);
            fallback.setCreateTime(LocalDateTime.now());

            int result = logisticNotifyMessageFallbackMapper.insert(fallback);
            if (result > 0) {
                log.info("物流消息已进入兜底表 logistic_notify_message_fallback，notifyId: {}",
                        message.getNotifyId());
            }
        } catch (Exception e) {
            log.error("存入兜底表 logistic_notify_message_fallback 失败", e);
        }
    }


    /**
     * 发送到 RabbitMQ（持久化兜底）
     */
    private void sendToMQ(LogisticNotifyMessage message, String exchange, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("MQ 消息发送成功，exchange: {}, routingKey: {}", exchange, routingKey);
        } catch (Exception e) {
            log.error("MQ 发送失败，需要记录到数据库兜底", e);
            saveToFallbackTable(message);
        }
    }















}