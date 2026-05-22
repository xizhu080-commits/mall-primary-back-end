package com.mall.demo.common.config;

import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.common.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {



    @Autowired
    private  JwtUtils jwtUtils;



    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 WebSocket 连接端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")

                // ✅ 添加握手拦截器，用于绑定 userId 到 Session
             // ... existing code ...
                // ✅ 添加握手拦截器，用于绑定 userId 到 Session
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        if (request instanceof ServletServerHttpRequest) {
                            // 从 URL 参数中获取 userId（前端连接时会带上 ?userId=xxx）
                            String userId = ((ServletServerHttpRequest) request)
                                    .getServletRequest().getParameter("userId");

                            if (userId != null) {
                                // 将 userId 存入 attributes，Spring 会自动将其作为 Principal 的 Name
                                attributes.put("userId", userId);
                                log.info("✅ WebSocket 握手成功，绑定用户: {}", userId);
                            } else {
                                log.warn("⚠️ WebSocket 握手时未找到 userId 参数");
                            }

                            // ✅ 新增：获取 shopId（商家发送消息时使用）
                            String shopId = ((ServletServerHttpRequest) request)
                                    .getServletRequest().getParameter("shopId");
                            if (shopId != null) {
                                attributes.put("shopId", shopId);

                            }
                        }

                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                    }
                })
// ... existing code ...

                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        String userId = (String) attributes.get("userId");
                        if (userId != null) {
                            log.info("设置 Principal: {}", userId);
                            return () -> userId;
                        }
                        return null;
                    }
                })

                .withSockJS()

        ;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // ✅ 修正：/user 不应该放在这里，它是特殊前缀，不是 Broker 路径
        registry.enableSimpleBroker("/topic", "/queue");

        // 客户端发送消息前缀
        registry.setApplicationDestinationPrefixes("/app");

        // 点对点前缀（Spring 会根据 Principal 自动路由）
        registry.setUserDestinationPrefix("/user");
    }



// ... existing code ...
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(new ChannelInterceptor() {

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

                // CONNECT阶段
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    // 1. 获取token
                    String token =
                            accessor.getFirstNativeHeader("Authorization");

                    log. info("token : {}", token);

                    if (token != null && token.startsWith("Bearer ")) {

                        token = token.substring(7);
                        // 2. 解析JWT
                        String userId = jwtUtils.getIdFromToken(token);

                        log.info("userId : {}", userId);

                        // 3. 绑定Principal
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userId,
                                        null,
                                        Collections.emptyList()
                                );

                        accessor.setUser(authentication);

                        log.info("WebSocket用户绑定成功，userId: {}, token: {}", userId, token);

                        // ✅ 新增：从握手属性中获取 shopId 并设置到 SecurityContext
                        String shopId = (String) accessor.getSessionAttributes().get("shopId");
                        if (shopId != null) {
                            SecurityUtils.setCurrentShopId(shopId);
                            log.info("✅ 设置当前店铺ID: {}", shopId);
                        }
                    }
                }

                return message;
            }
        });
    }
// ... existing code ...


}
