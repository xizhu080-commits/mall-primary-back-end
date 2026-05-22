package com.mall.demo.module.messageRecord.service;


import com.mall.demo.module.messageRecord.dto.req.SendMessageReqDto;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.entity.UserSession;

import java.util.List;

public interface MessageRecordService {

    // 读取消息
  void markAllAsRead(String sessionId);

  // 返回当前用户指定对话框消息列表
    List<MessageRecord> getMessageListBySessionId(int limit, String beforeId, String sessionId);

    //返回当前用户所有对话框
    List<UserSession> getSessionIds(String shopId);



    // 发送群聊消息
    void sendPublicWeChatMessage(SendMessageReqDto dto);



    // 发送私聊消息
    void sendPrivateWeChatMessageTest(SendMessageReqDto dto,String messagePublisherId);


    //加入聊天
    void addWeChat(String userId);



    // 测试发送消息
    void sendWeChatMessageTest(MessageRecord messageRecord);

}
