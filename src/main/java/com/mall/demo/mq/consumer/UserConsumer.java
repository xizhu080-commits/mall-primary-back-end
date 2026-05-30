/*
package com.mall.demo.mq.consumer;


import com.alibaba.fastjson2.JSON;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserConsumer {


   private final RedisService redisService;

    // 消费者1：专门处理注册后的“存入缓存”
    @RabbitListener(queues = "user.register.queue")
    public void handleCache(User user) {
        log.info("【缓存中心】正在将用户 {} 存入缓存...", user.getUserId());
        try {
            String json = JSON.toJSONString(user);
            redisService.set("user:" + user.getUserId(), json, 7);
        }catch (Exception e){
            throw new RuntimeException("【缓存中心】存入缓存失败");
        }
    }
}
*/



package com.mall.demo.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.module.user.entity.User;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserConsumer {

    private final RedisService redisService;

    @RabbitListener(queues = "user.register.queue")
    public void handleCache(User user, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (user == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("【缓存中心】正在将用户 {} 存入缓存...", user.getUserId());
            String json = JSON.toJSONString(user);
            redisService.set("user:" + user.getUserId(), json, 7);
            log.info("【缓存中心】用户 {} 存入缓存成功！", user.getUserId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("【缓存中心】用户缓存处理失败: {}, 错误: {}", user != null ? user.getUserId() : "null", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }
}

