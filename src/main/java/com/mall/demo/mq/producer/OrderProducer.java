package com.mall.demo.mq.producer;

import com.mall.demo.module.order.entity.MyOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@Service
public class OrderProducer {

    private final RabbitTemplate rabbitTemplate;
    public void sendPayment(MyOrder myOrder) {

        log.info("[生产者中心]发送订单支付消息：{}", myOrder.getOrderId());

        long ttl = Duration.between(LocalDateTime.now(), myOrder.getExpireTime()).toMillis();
        rabbitTemplate.convertAndSend(
                "order.ttl.exchange",
                "order.ttl.key",
                myOrder.getOrderId(),
                message ->  {

                    message.getMessageProperties().setExpiration(String.valueOf(ttl));
                    return message;
                }
                );
    }



}
