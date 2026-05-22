package com.mall.demo.mq.producer;


import com.mall.demo.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;  // 添加这个import
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;  // 添加这个注解

@Slf4j  // 添加这个注解
@Component  // 让Spring管理这个Bean
@RequiredArgsConstructor
public class UserProducer {

    private final RabbitTemplate rabbitTemplate;


    // 注册
    public void sendRegister(User user) {


        rabbitTemplate.convertAndSend(
                "user-exchange",
                "user.register.key",
                user);
        log.info("【用户注册】消息已发送到交换机 user-exchange，路由键: user.register.key，用户ID: {}", user.getUserId());

    }


}