package com.mall.demo.module.messageRecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.messageRecord.dto.req.CreateSessionReqDto;
import com.mall.demo.module.messageRecord.dto.resp.CreateSessionRespDto;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.entity.UserSession;
import com.mall.demo.module.messageRecord.mapper.MessageRecordMapper;
import com.mall.demo.module.messageRecord.mapper.UserSessionMapper;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import com.mall.demo.module.user.entity.User;
import com.mall.demo.module.user.mapper.UserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Data
public class UserSessionServiceImpl implements UserSessionService {


    private final UserSessionMapper userSessionMapper;
    private final MessageRecordMapper messageRecordMapper;
    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private static final String SEPARATOR = "::";
    private final RedisService redisService;

    /**
     * 更新或创建会话表
     *
     * @param currentUserId        当前用户ID（会话所属者）
     * @param partnerId            对方ID
     * @param message              刚发送/收到的消息
     * @param currentPartnerName   当前用户视角下对方的名称
     * @param currentPartnerAvatar 当前用户视角下对方的头像
     * @param partnerPartnerName   对方视角下当前用户的名称
     * @param partnerPartnerAvatar 对方视角下当前用户的头像
     */
    public void upsertSession(String currentUserId, String partnerId, MessageRecord message, String sessionId,
                              String currentPartnerName, String currentPartnerAvatar,
                              String partnerPartnerName, String partnerPartnerAvatar) {
        log.info("发送方id:{}", currentUserId);
        log.info("接收方id:{}", partnerId);

        // 1. 更新当前用户的会话记录（不增加未读数）
        updateOrCreate(currentUserId, sessionId, partnerId, message, currentPartnerName, currentPartnerAvatar, false);

        // 2. 更新对方的会话记录（未读数+1）
        updateOrCreate(partnerId, sessionId, currentUserId, message, partnerPartnerName, partnerPartnerAvatar, true);

    }







    public void updateOrCreate(String userId, String sessionId, String partnerId, MessageRecord message, String partnerName, String partnerAvatar, boolean shouldIncrementUnread) {
        UserSession existing = userSessionMapper.selectByUserIdAndSessionId(userId, sessionId);

        if (existing == null) {
            UserSession newSession = new UserSession();
            newSession.setUserId(userId);
            newSession.setSessionId(sessionId);
            newSession.setPartnerId(partnerId);
            newSession.setPartnerName(partnerName);

            if (message != null) {
                newSession.setPartnerType(message.getMessagePublisherType());
                newSession.setLastMessage(message.getContent());
                newSession.setLastMessageTime(message.getCreateTime());
            } else {
                newSession.setPartnerType("SYSTEM");
                newSession.setLastMessage(null);
                newSession.setLastMessageTime(LocalDateTime.now());
            }

            newSession.setPartnerAvatar(partnerAvatar);
            newSession.setUnreadCount(shouldIncrementUnread ? 1 : 0);
            userSessionMapper.insert(newSession);
        } else {
            if (message != null) {
                existing.setLastMessage(message.getContent());
                existing.setLastMessageTime(message.getCreateTime());
                userSessionMapper.updateById(existing);
            }

            if (shouldIncrementUnread && message != null) {
                userSessionMapper.incrementUnreadCount(userId, sessionId);
                log.info("id为:" + userId + "的用户的消息未读数+1");
            }
        }
    }


















        @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateSessionRespDto createSession(CreateSessionReqDto dto) {

        String partnerType = dto.getPartnerType();

        if ("MERCHANT".equalsIgnoreCase(partnerType)) {
            return createUserToShopSession(dto);
        } else if ("USER".equalsIgnoreCase(partnerType)) {
            return createShopToUserSession(dto);
        } else {
            log.error("不支持的partnerType: {}", partnerType);
            throw new IllegalArgumentException("不支持的会话类型: " + partnerType);
        }
    }

      /**
     * 用户联系客服（获取会话信息，不操作数据库）
     */
    private CreateSessionRespDto createUserToShopSession(CreateSessionReqDto dto) {
        String currentUserId = SecurityUtils.getId();
        String partnerId = dto.getPartnerId();
        String partnerName = dto.getPartnerName();
        String partnerAvatar = dto.getPartnerAvatar();

        if (currentUserId == null) {
            log.error("当前用户未登录");
            throw new IllegalArgumentException("当前用户未登录");
        }

        if (partnerId == null || partnerId.isEmpty()) {
            log.error("对方ID不能为空");
            throw new IllegalArgumentException("对方ID不能为空");
        }

        String shopCacheKey = "shop:" + partnerId;

        Shop shop = redisService.get(
            shopCacheKey,
            Shop.class,
            (key) -> {
                log.info("缓存未命中，从数据库查询店铺信息，shopId: {}", partnerId);
                return shopMapper.selectById(partnerId);
            },
            60
        );

        if (shop == null) {
            log.error("店铺不存在，shopId: {}", partnerId);
            throw new IllegalArgumentException("店铺不存在");
        }

        partnerName = shop.getShopName() != null ? shop.getShopName() : partnerName;
        partnerAvatar = shop.getShopLogo() != null ? shop.getShopLogo() : partnerAvatar;

        List<String> ids = java.util.Arrays.asList(currentUserId, partnerId);
        ids.sort(String::compareTo);
        String sessionId = ids.get(0) + SEPARATOR + ids.get(1);

        log.info("用户{}获取客服{}的会话信息，sessionId: {}", currentUserId, partnerId, sessionId);

        CreateSessionRespDto respDto = new CreateSessionRespDto();
        respDto.setSessionId(sessionId);
        respDto.setMessagePublisherId(currentUserId);
        respDto.setPartnerId(partnerId);
        respDto.setPartnerName(partnerName);
        respDto.setPartnerAvatar(partnerAvatar);
        respDto.setPartnerType("MERCHANT");
        respDto.setExist(false);

        return respDto;
    }

    /**
     * 商家联系用户（获取会话信息，不操作数据库）
     */
    private CreateSessionRespDto createShopToUserSession(CreateSessionReqDto dto) {
        String currentUserId = SecurityUtils.getId();
        String currentUserType = SecurityUtils.getType();
        String partnerId = dto.getPartnerId();
        String partnerName = dto.getPartnerName();
        String partnerAvatar = dto.getPartnerAvatar();

        if (currentUserId == null) {
            log.error("当前用户未登录");
            throw new IllegalArgumentException("当前用户未登录");
        }

        if (partnerId == null || partnerId.isEmpty()) {
            log.error("对方ID不能为空");
            throw new IllegalArgumentException("对方ID不能为空");
        }

        String shopId = dto.getShopId();



        String shopCacheKey = "shop:" + shopId;

        Shop shop = redisService.get(
            shopCacheKey,
            Shop.class,
            (key) -> {
                log.info("缓存未命中，从数据库查询店铺信息，shopId: {}", shopId);
                return shopMapper.selectById(shopId);
            },
            60
        );

        if (shop == null) {
            log.error("店铺不存在，shopId: {}", shopId);
            throw new IllegalArgumentException("店铺不存在");
        }

        String userCacheKey = "user:" + partnerId;

        User user = redisService.get(
            userCacheKey,
            User.class,
            (key) -> {
                log.info("缓存未命中，从数据库查询用户信息，userId: {}", partnerId);
                return userMapper.selectById(partnerId);
            },
            60
        );

        if (user == null) {
            log.error("用户不存在，userId: {}", partnerId);
            throw new IllegalArgumentException("用户不存在");
        }

        partnerName = user.getUserName() != null ? user.getUserName() : partnerName;
        partnerAvatar = user.getUserAvatar() != null ? user.getUserAvatar() : partnerAvatar;

        List<String> ids = java.util.Arrays.asList(shopId, partnerId);
        ids.sort(String::compareTo);
        String sessionId = ids.get(0) + SEPARATOR + ids.get(1);

        log.info("商家{}获取用户{}的会话信息，sessionId: {}", shopId, partnerId, sessionId);

        CreateSessionRespDto respDto = new CreateSessionRespDto();
        respDto.setSessionId(sessionId);
        respDto.setMessagePublisherId(currentUserId);
        respDto.setPartnerId(partnerId);
        respDto.setPartnerName(partnerName);
        respDto.setPartnerAvatar(partnerAvatar);
        respDto.setPartnerType("USER");
        respDto.setExist(false);

        return respDto;
    }





















}







