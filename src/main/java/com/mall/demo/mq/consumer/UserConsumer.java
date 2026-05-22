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
