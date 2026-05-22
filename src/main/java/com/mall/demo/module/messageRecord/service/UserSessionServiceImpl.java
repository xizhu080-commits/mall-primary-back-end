package com.mall.demo.module.messageRecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.messageRecord.dto.req.CreateSessionReqDto;
import com.mall.demo.module.messageRecord.dto.resp.CreateSessionRespDto;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.entity.UserSession;
import com.mall.demo.module.messageRecord.mapper.UserSessionMapper;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Data
public class UserSessionServiceImpl implements UserSessionService {



    private final UserSessionMapper userSessionMapper;
    private final ShopMapper shopMapper;
    private static final String SEPARATOR = "::";











        /**
         * 更新或创建会话表
         * @param currentUserId 当前用户ID（发送方或接收方）
         * @param partnerId 对方ID
         * @param message 刚发送/收到的消息
         */
        public void upsertSession(String currentUserId, String partnerId, MessageRecord message,String sessionId, String partnerName,String partnerAvatar) {

            // 1. 更新当前用户的会话记录（发送方，不增加未读数）
            updateOrCreate(currentUserId, sessionId, partnerId, message, partnerName, partnerAvatar, false);

            // 2. 更新对方的会话记录（接收方，未读数+1）
            updateOrCreate(partnerId, sessionId, currentUserId, message, partnerName, partnerAvatar, true);

        }

        private void updateOrCreate(String userId, String sessionId, String partnerId, MessageRecord message, String partnerName,String partnerAvatar, boolean shouldIncrementUnread) {
            UserSession existing = userSessionMapper.selectByUserIdAndSessionId(userId, sessionId);

            if (existing == null) {
                // 不存在 → 插入新记录
                UserSession newSession = new UserSession();
                // 自己
                newSession.setUserId(userId);
                newSession.setSessionId(sessionId);
                // 对方:id name avatar
                newSession.setPartnerId(partnerId);
                newSession.setPartnerName(partnerName);
                newSession.setPartnerType(message.getMessagePublisherType());
                newSession.setPartnerAvatar(partnerAvatar);
                newSession.setLastMessage(message.getContent());
                newSession.setLastMessageTime(message.getCreateTime());
                newSession.setUnreadCount(shouldIncrementUnread ? 1 : 0);
                userSessionMapper.insert(newSession);
            } else {
                // 已存在 → 更新最后消息和时间
                existing.setLastMessage(message.getContent());
                existing.setLastMessageTime(message.getCreateTime());
                userSessionMapper.updateById(existing);

                // 如果是接收方，未读数+1
                if (shouldIncrementUnread) {
                    userSessionMapper.incrementUnreadCount(userId, sessionId);
                }
            }
        }
















        // 创建会话:联系客服

        @Override
        public CreateSessionRespDto createSession(CreateSessionReqDto dto) {

            String currentUserId = SecurityUtils.getId();

            String partnerId = dto.getPartnerId();
            String partnerName = dto.getPartnerName();
            String partnerAvatar = dto.getPartnerAvatar();
            String partnerType = dto.getPartnerType();
            boolean exist = false;
            if (currentUserId == null) {
                log.error("当前用户未登录");
                throw new IllegalArgumentException("当前用户未登录");

            }


            Shop shop = shopMapper.selectById(partnerId);
            if (shop == null) {
                log.error("店铺不存在");
                throw new IllegalArgumentException("店铺不存在");
            }

            List<String> ids = java.util.Arrays.asList(currentUserId, partnerId);
            ids.sort(String::compareTo);


            String sessionId = ids.get(0) + SEPARATOR + ids.get(1);
            upsertSession(currentUserId, partnerId, null, sessionId, partnerName, partnerAvatar);

            LambdaQueryWrapper<UserSession> queryWrapper = new LambdaQueryWrapper<>();

            queryWrapper.eq(UserSession::getUserId, currentUserId);
            queryWrapper.eq(UserSession::getSessionId, sessionId);
            queryWrapper.eq(UserSession::getPartnerId, partnerId);
            queryWrapper.select(UserSession::getId);
            UserSession existing = userSessionMapper.selectOne(queryWrapper);
            if (existing == null) {
                log.info("会话不存在");
                exist = false;
            } else {
                log.info("会话存在");
                exist = true;
            }

            CreateSessionRespDto createSessionRespDto = new CreateSessionRespDto();

            createSessionRespDto.setSessionId(sessionId);
            createSessionRespDto.setCurrentUserId(currentUserId);
            createSessionRespDto.setPartnerId(partnerId);
            createSessionRespDto.setPartnerName(partnerName);
            createSessionRespDto.setPartnerAvatar(partnerAvatar);
            createSessionRespDto.setPartnerType(partnerType);
            createSessionRespDto.setExist(exist);

            return createSessionRespDto;
        }







    }






