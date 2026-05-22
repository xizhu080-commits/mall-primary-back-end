package com.mall.demo.security.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message,
                              MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        // STOMP CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String token =
                    accessor.getFirstNativeHeader("Authorization");

            log.info("WebSocket CONNECT token: {}", token);

            // TODO:
            // 1. 校验 token
            // 2. 解析 userId
            // 3. 绑定 Principal
        }

        return message;
    }
}