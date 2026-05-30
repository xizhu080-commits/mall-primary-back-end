package com.mall.demo.module.messageRecord.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.mall.demo.common.config.RabbitMQConfig;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.Refund.entity.RefundNotifyMessage;
import com.mall.demo.module.Refund.mapper.RefundNotifyMessageMapper;
import com.mall.demo.module.logistic.entity.LogisticNotifyMessage;
import com.mall.demo.module.logistic.mapper.LogisticNotifyMessageMapper;
import com.mall.demo.module.messageRecord.dto.req.SendMessageReqDto;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.entity.MessageRecordFallback;
import com.mall.demo.module.messageRecord.entity.UserSession;
import com.mall.demo.module.messageRecord.mapper.MessageRecordFallbackMapper;
import com.mall.demo.module.messageRecord.mapper.MessageRecordMapper;
import com.mall.demo.module.messageRecord.mapper.UserSessionMapper;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.module.payment.entity.Payment;
import com.mall.demo.module.payment.entity.PaymentNotifyFallback;
import com.mall.demo.module.payment.entity.PaymentNotifyMessage;
import com.mall.demo.module.payment.mapper.PaymentNotifyMessageMapper;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import com.mall.demo.module.user.entity.User;
import com.mall.demo.module.user.mapper.UserMapper;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import io.jsonwebtoken.lang.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageRecordServiceImpl implements MessageRecordService {
    private final SuborderMapper suborderMapper;
    private final MessageRecordMapper messageRecordMapper;
    private final MessageRecordFallbackMapper messageRecordFallbackMapper;
    private final SimpMessagingTemplate websocket;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LogisticNotifyMessageMapper logisticNotifyMessageMapper;
    private final PaymentNotifyMessageMapper paymentNotifyMessageMapper;
    private final RefundNotifyMessageMapper refundNotifyMessageMapper;
    private final UserSessionMapper userSessionMapper;
    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final UserSessionService userSessionService;



    private static final String SEPARATOR = "::";


/*
* 标记消息为已读
* */
    @Override
    public void markAllAsRead(String sessionId ,String shopId) {
        log.info("==============================================已经进入会话标记区域===============================================");
        String currentUserId = SecurityUtils.getId();
        if (shopId != null) {
            messageRecordMapper.markAllAsRead(sessionId, shopId);
            userSessionMapper.clearUnreadCount(shopId, sessionId);

            log.info("店铺ID{}:  标记会话为已读", shopId);

        }
        else if (currentUserId != null){

            messageRecordMapper.markAllAsRead(sessionId, currentUserId);
            userSessionMapper.clearUnreadCount(currentUserId, sessionId);
            log.info("用户ID{}:  标记会话为已读", currentUserId);

        }
        else{
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }





    }




    /*
     * 返回当前用户所有对话框
     * */
    @Override
    public List<UserSession> getSessionIds(String shopId) {
        String currentUserId;

        if (shopId != null && !shopId.isEmpty()){
            currentUserId = shopId;
        }else {
            currentUserId = SecurityUtils.getId();
        }





        if (currentUserId == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }
        // 调用示例
        List<UserSession> sessionIds = userSessionMapper.getSessionIds(currentUserId);
        return sessionIds;
    }






    /*
    * 返回当前用户指定对话框消息列表
    * */
    @Override
    public List<MessageRecord> getMessageListBySessionId(int limit, String beforeId, String sessionId) {

        String currentUserId = SecurityUtils.getId();
        if (currentUserId == null) {
          throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }
        // 调用示例
        List<MessageRecord> messageRecordList = messageRecordMapper.getMessageRecordList(sessionId, beforeId, limit);

        return messageRecordList;
    }






































// =======================================================================================当前用户发送消息给其他人(非退款、支付、物流)==================================================================================================================

    /*
    * 发送群聊消息(非退款、支付、物流)
    * */
    @Override
    public void sendPublicWeChatMessage(SendMessageReqDto dto) {
        String messagePublisherId = SecurityUtils.getId();

        String messageRecordId = IdUtil.getSnowflakeNextIdStr();

        String messagePublisherType = dto.getMessagePublisherType();
        String targetUserId = dto.getTargetUserId();
        String targetUserType = dto.getTargetUserType();
        String content = dto.getContent();


        log.info("{}: {} 发送消息给群聊 {}，目标用户类型：{}",
                messagePublisherType, messagePublisherId, targetUserId, targetUserType);

        if (targetUserId == null || targetUserId.isEmpty()){
            throw new RuntimeException("目标群聊ID不能为空");
        }

        MessageRecord messageRecord = new MessageRecord();
        messageRecord.setMessageRecordId(messageRecordId);
        messageRecord.setMessagePublisherId(messagePublisherId);
        messageRecord.setMessagePublisherType(messagePublisherType);
        messageRecord.setTargetUserId(targetUserId);
        messageRecord.setTargetUserType(targetUserType);
        messageRecord.setContent(content);
        messageRecord.setIsRead(false);
        messageRecord.setReadTime(LocalDateTime.now());
        messageRecord.setCreateTime(LocalDateTime.now());

        // 1. 保存到数据库
        saveToMessageTable(messageRecord);

        // 2. WebSocket 实时推送
        boolean wsSuccess = sendWebSocket(messageRecord);

        // 3. MQ 兜底（用户离线时重新推送）
        if (!wsSuccess) {
            // ✅ 修正：使用独立的消息记录交换机
            sendToMQ(messageRecord, RabbitMQConfig.MESSAGE_RECORD_EXCHANGE,
                    getChatRoutingKey(messagePublisherType, targetUserType));
        }

        // 4. Redis 存储用户站内信
        storeToRedis(messageRecord);

        // 5. 持久化推送状态
        updatePushStatus(messageRecordId, wsSuccess);
    }







    /*
    * 发送私聊消息
    * */
 // ... existing code ...
    /*
    * 发送私聊消息
    * */
    @Override
    public void sendPrivateWeChatMessageTest(SendMessageReqDto dto,String messagePublisherId) {

        if (messagePublisherId == null) {
            log.error("发送方ID为空");
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }

        // ✅ 新增：记录发送方类型和 ID
        log.info("✅ 发送私聊消息 - 发送方类型: {}, 发送方ID: {}, 接收方类型: {}, 接收方ID: {}",
                dto.getMessagePublisherType(), messagePublisherId,
                dto.getTargetUserType(), dto.getTargetUserId());

        String messageRecordId = IdUtil.getSnowflakeNextIdStr();

        String messagePublisherType = dto.getMessagePublisherType();
        String targetUserId = dto.getTargetUserId();
        String targetUserType = dto.getTargetUserType();
        String content = dto.getContent();



        List<String> ids = java.util.Arrays.asList(targetUserId, messagePublisherId);
        ids.sort(String::compareTo);

        String sessionId = ids.get(0) + SEPARATOR + ids.get(1);

        log.info("{}: {} 发送消息给用户 {}，目标用户类型：{}",
                messagePublisherType, messagePublisherId, targetUserId, targetUserType);

        if (targetUserId == null || targetUserId.isEmpty()){
            throw new RuntimeException("目标用户ID不能为空");
        }

        MessageRecord messageRecord = new MessageRecord();
        messageRecord.setMessageRecordId(messageRecordId);
        messageRecord.setSessionId(sessionId);
        messageRecord.setMessagePublisherId(messagePublisherId);
        messageRecord.setMessagePublisherType(messagePublisherType);
        messageRecord.setTargetUserId(targetUserId);
        messageRecord.setTargetUserType(targetUserType);
        messageRecord.setContent(content);
        messageRecord.setIsRead(false);
        messageRecord.setReadTime(LocalDateTime.now());
        messageRecord.setCreateTime(LocalDateTime.now());

        // 1. 保存到数据库
        saveToMessageTable(messageRecord);

        // 2. WebSocket 实时推送
        boolean wsSuccess = sendWebSocket(messageRecord);

        // 3. MQ 兜底（用户离线时重新推送）
        if (!wsSuccess) {
            // ✅ 修正：使用独立的消息记录交换机
            sendToMQ(messageRecord, RabbitMQConfig.MESSAGE_RECORD_EXCHANGE,
                    getChatRoutingKey(messagePublisherType, targetUserType));
        }

        // 4. Redis 存储用户站内信
        storeToRedis(messageRecord);

        // 5. 持久化推送状态
        updatePushStatus(messageRecordId, wsSuccess);
    }
// ... existing code ...




    /*
    * 加入聊天室
    * */
    @Override
    public void addWeChat(String userId) {

    }




    /*
    * 测试发送消息
    * */
    @Override
    public void sendWeChatMessageTest(MessageRecord messageRecord) {


        log.info("========== 测试推送 ==========");
        log.info("测试推送给用户: {}", messageRecord.getTargetUserId());



        try {
            websocket.convertAndSendToUser(messageRecord.getTargetUserId(), "/queue/userWeChatMessage", messageRecord);
            log.info("测试消息推送成功给用户: {}", messageRecord.getTargetUserId());
        } catch (Exception e) {
            log.error("测试消息推送失败", e);
        }

    }






    /**
     * 获取对话消息的路由键
     */
    private String getChatRoutingKey(String publisherType, String targetType) {
        // 根据目标用户类型决定路由键
        if (publisherType.equals(targetType)) {
            return RabbitMQConfig.MERCHANT_MESSAGE_KEY;
        } else {
            return RabbitMQConfig.USER_MESSAGE_KEY;
        }
    }

    /**
     * WebSocket 推送（实时）
     */
    private boolean sendWebSocket(MessageRecord messageRecord) {
        try {
            // ✅ 根据目标用户类型选择不同队列
            String destination = "MERCHANT".equals(messageRecord.getTargetUserType())
                    ? "/queue/merchantWeChatMessage"
                    : "/queue/userWeChatMessage";

            // ✅ 关键修复：前端商家连接时用 shopId 作为 userId，所以这里也用 shopId 作为目标
            // convertAndSendToUser 的第一个参数是 principal name，需要与前端连接时使用的 userId 一致
            String targetUserId = messageRecord.getTargetUserId();

            log.info("接收方ID=====================targetUserId:{}", targetUserId);
            websocket.convertAndSendToUser(
                    targetUserId,
                    destination,
                    messageRecord
            );

            log.info("后端推送路径:{}", destination);
            //          /user/ 前缀是 Spring STOMP 框架自动添加的，实际订阅路径已经包含在 destination 变量中。
            log.info("{}: {} 向 {}: {} 发送消息成功, 前端WebSocket队列: /user{}",
                    messageRecord.getMessagePublisherType(),
                    messageRecord.getMessagePublisherId(),
                    messageRecord.getTargetUserType(),
                    messageRecord.getTargetUserId(),
                    destination);

            return true;
        } catch (Exception e) {
            log.error("{}: {} 向 {}: {} 推送消息失败: {}",
                    messageRecord.getMessagePublisherType(),
                    messageRecord.getMessagePublisherId(),
                    messageRecord.getTargetUserType(),
                    messageRecord.getTargetUserId(),
                    e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送到 RabbitMQ（持久化兜底）
     */
    private void sendToMQ(MessageRecord messageRecord, String exchange, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, messageRecord);
            log.info("MQ 消息发送成功，exchange: {}, routingKey: {}", exchange, routingKey);
        } catch (Exception e) {
            log.error("MQ 发送失败，需要记录到数据库兜底", e);
            saveToFallbackTable(messageRecord);
        }
    }

    /**
     * 存 Redis（商家/用户轮询用）
     */
    private void storeToRedis(MessageRecord messageRecord) {
        try {
            String key = String.format("message:%s:%s:list",
                    messageRecord.getTargetUserType().toLowerCase(),
                    messageRecord.getTargetUserId());

            String json = objectMapper.writeValueAsString(messageRecord);
            redisTemplate.opsForList().leftPush(key, json);
            redisTemplate.opsForList().trim(key, 0, 99);
            redisTemplate.expire(key, 7, TimeUnit.DAYS);

            log.info("消息已存入 Redis, Key: {}", key);
        } catch (Exception e) {
            log.error("Redis 存储失败，Key: {}", messageRecord.getTargetUserId(), e);
        }
    }

  // ... existing code ...
    /**
     * 存入消息表（MySQL - message_record,user_session）
     */
    private void saveToMessageTable(MessageRecord messageRecord) {
        try {
            int result = messageRecordMapper.insert(messageRecord);


            log.info("发送方ID:{}", messageRecord.getMessagePublisherId());
            log.info("接收方ID:{}", messageRecord.getTargetUserId());


            String userNameA = "";
            String userNameB = "";

            String userAvatarA = "";
            String userAvatarB = "";

            // ✅ 从用户表/店铺表查询接收方信息
            User userA = userMapper.selectById(messageRecord.getTargetUserId());
            if(userA == null) {
                //当前接收方为商家/店铺
                Shop shop = shopMapper.selectById(messageRecord.getTargetUserId());
                if (shop != null) {
                    log.info("✅ 接收方为店铺 - 店铺名称:{}, 店铺ID:{}", shop.getShopName(), shop.getShopId());
                    userNameA = shop.getShopName();
                    userAvatarA = shop.getShopLogo();
                } else {
                    log.warn("⚠️ 未找到接收方信息，targetUserId: {}", messageRecord.getTargetUserId());
                    userNameA = "未知用户";
                    userAvatarA = "";
                }
            }
            else {
                log.info("✅ 接收方为用户 - 用户名称:{}", userA.getUserName());
                userNameA = userA.getUserName();
                userAvatarA = userA.getUserAvatar();
            }

            // ✅ 从用户表/店铺表查询发送方信息
            User userB = userMapper.selectById(messageRecord.getMessagePublisherId());
            if(userB == null){
                Shop shop = shopMapper.selectById(messageRecord.getMessagePublisherId());
                if (shop != null) {
                    log.info("✅ 发送方为店铺 - 店铺名称:{}, 店铺ID:{}", shop.getShopName(), shop.getShopId());
                    userNameB = shop.getShopName();
                    userAvatarB = shop.getShopLogo();
                } else {
                    log.warn("⚠️ 未找到发送方信息，messagePublisherId: {}", messageRecord.getMessagePublisherId());
                    userNameB = "未知用户";
                    userAvatarB = "";
                }
            }else {
                log.info("✅ 发送方为用户 - 用户名称:{}", userB.getUserName());
                userNameB = userB.getUserName();
                userAvatarB = userB.getUserAvatar();
            }

            // ✅ 修正：使用 equals() 比较字符串
            if (userNameA.isEmpty() && userNameB.isEmpty()){
                throw new Exception("用户不存在");
            }

            log.info("发送方id:{}", messageRecord.getMessagePublisherId());
            log.info("接收方id:{}", messageRecord.getTargetUserId());

            // 更新/创建会话表: 一次调用同时更新发送方和接收方视角的会话
            // 发送方视角：对方是接收方，显示接收方的名称和头像
            // 接收方视角：对方是发送方，显示发送方的名称和头像
            userSessionService.upsertSession(
                    messageRecord.getMessagePublisherId(),
                    messageRecord.getTargetUserId(),
                    messageRecord,
                    messageRecord.getSessionId(),
                    userNameA,   // 发送方视角下对方的名称（接收方名称）
                    userAvatarA, // 发送方视角下对方的头像（接收方头像）
                    userNameB,   // 接收方视角下对方的名称（发送方名称）
                    userAvatarB  // 接收方视角下对方的头像（发送方头像）
            );










            if (result > 0) {
                log.info("✅ 消息已存入 message_record 表，messageRecordId: {}",
                        messageRecord.getMessageRecordId());
            }
        } catch (Exception e) {
            log.error("❌ 存入 message_record 表和 user_session 表失败，messageRecordId: {}",
                    messageRecord.getMessageRecordId(), e);
            throw new RuntimeException("保存消息失败", e);
        }
    }
// ... existing code ...

    /**
     * 更新推送状态
     */
    private void updatePushStatus(String messageRecordId, boolean success) {
        try {
            messageRecordMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MessageRecord>()
                            .set(MessageRecord::getPushStatus, success ? 1 : 2)
                            .set(MessageRecord::getRetryCount, success ? 0 : 1)
                            .eq(MessageRecord::getMessageRecordId, messageRecordId)
            );
            log.info("更新推送状态成功, messageRecordId: {}, status: {}",
                    messageRecordId, success ? 1 : 2);
        } catch (Exception e) {
            log.error("更新推送状态失败", e);
        }
    }

    /* 终极兜底：存 fallback 表（MySQL - message_record_fallback）
     * 定时任务会扫描这张表，重试发送到 MQ
     */
    private void saveToFallbackTable(MessageRecord messageRecord) {
        try {
            String messageJson = objectMapper.writeValueAsString(messageRecord);

            MessageRecordFallback fallback = new MessageRecordFallback();
            fallback.setMessageJson(messageJson);
            fallback.setRetryCount(0);
            fallback.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
            fallback.setStatus(0);
            fallback.setCreateTime(LocalDateTime.now());

            int result = messageRecordFallbackMapper.insert(fallback);
            if (result > 0) {
                log.info("消息已进入兜底表 message_record_fallback，messageRecordId: {}",
                        messageRecord.getMessageRecordId());
            }
        } catch (Exception e) {
            log.error("存入 message_record_fallback 兜底表失败", e);
        }
    }












}
