package com.mall.demo.module.messageRecord.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.messageRecord.dto.req.SendMessageReqDto;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import com.mall.demo.module.messageRecord.entity.UserSession;
import com.mall.demo.module.messageRecord.service.MessageRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;


import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/messageRecord")
@RequiredArgsConstructor
@Slf4j
public class MessageRecordController {
    private final MessageRecordService messageRecordService;


    // 返回当前用户指定对话框消息列表
    @GetMapping("/getMessageListBySessionId")
    public RestResp<List<MessageRecord>> getMessageListByUserId(@RequestParam(defaultValue = "20") int limit,
                                                               @RequestParam(required = false) String beforeId,
                                                               @RequestParam String sessionId) {


        log.info("==============================================前端传来的beforeId: {} ==== limit: {}===========================================", beforeId, limit);

        return RestResp.ok(messageRecordService.getMessageListBySessionId(limit, beforeId, sessionId));
    }

    // 返回当前用户所有对话框
    @GetMapping("/getSessionIds")
    public RestResp<List<UserSession>> getSessionIds(@RequestParam(required = false) String shopId) {
        return RestResp.ok(messageRecordService.getSessionIds(shopId));
    }



    // 标记消息为已读
    @PostMapping("/markAllAsRead")
    public RestResp<String> markAllAsRead(@RequestParam String sessionId,@RequestParam(required = false) String shopId) {

        log.info("==============================================准备进入会话标记区域===============================================");
        log.info("==============================================前端传来的sessionId: {} ===============================================", sessionId);



        messageRecordService.markAllAsRead(sessionId,shopId);
        return RestResp.ok("标记成功");
    }



// ... existing code ...
    // 发送私聊消息
    @MessageMapping("/sendPrivateMessage")
    public void sendPrivateWeChatMessage(@Payload SendMessageReqDto dto, Principal principal, SimpMessageHeaderAccessor headerAccessor) {

        // ✅ 核心：从 Principal 中获取发送方 ID (即 WebSocketConfig 中通过 JWT 解析出的 userId)
        if (principal != null) {
            String messagePublisherId = principal.getName();
            log.info("✅ WebSocket 识别到发送方 ID: {}", messagePublisherId);
            String shopId = (String) headerAccessor.getSessionAttributes().get("shopId");
            log.info("这里是controller当前店铺ID: {}", shopId);

            // ✅ 如果发送方是商家，需要将 merchantId 转换为 shopId
            String actualPublisherId = messagePublisherId;

            if ("MERCHANT".equals(dto.getMessagePublisherType())) {
                // 从 SecurityUtils 获取当前店铺ID（由拦截器从请求头设置）

                log.info("当前店铺ID: {}", shopId);
                if (shopId != null && !shopId.isEmpty()) {
                    actualPublisherId = shopId;
                } else {
                    log.warn("⚠️ 商家发送消息但未携带店铺ID，使用商家ID: {}", messagePublisherId);
                }
            }

            // 将实际发送方ID设置到 Service 层
            messageRecordService.sendPrivateWeChatMessageTest(dto, actualPublisherId);
        } else {
            log.error("❌ WebSocket Principal 为空，用户未认证或 Token 解析失败");
            throw new RuntimeException("用户未认证，请检查 Token 是否正确");
        }


    }
// ... existing code ...



}