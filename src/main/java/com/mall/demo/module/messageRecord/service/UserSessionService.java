package com.mall.demo.module.messageRecord.service;

import com.mall.demo.module.messageRecord.dto.req.CreateSessionReqDto;
import com.mall.demo.module.messageRecord.dto.resp.CreateSessionRespDto;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.entity.UserSession;

public interface UserSessionService {


    // 更新或创建会话表
      void upsertSession(String currentUserId, String partnerId, MessageRecord message, String sessionId,
                          String currentPartnerName, String currentPartnerAvatar,
                          String partnerPartnerName, String partnerPartnerAvatar);


      //主动创建会话:联系客服
      CreateSessionRespDto createSession(CreateSessionReqDto dto);

    // 更新或创建单条会话记录（供系统通知调用）
    void updateOrCreate(String userId, String sessionId, String partnerId, MessageRecord message,
                        String partnerName, String partnerAvatar, boolean shouldIncrementUnread);

}
