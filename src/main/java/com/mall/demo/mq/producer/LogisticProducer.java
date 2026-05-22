package com.mall.demo.mq.producer;

import com.mall.demo.module.logistic.entity.Logistic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;@Slf4j
@Service // 只保留 Service
@RequiredArgsConstructor
public class LogisticProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送派送完成消息（触发进入待签收状态）
     */
    public void sendSignMessage(Logistic logistic) {
        // 状态 0 才能进入后续流程
        if (logistic == null || logistic.getStatus() != 0) return;

        log.info("[物流消息] 发送派送倒计时，物流ID：{}", logistic.getLogisticId());

        // 计算从现在到配送完成（预计签收触发）的时间差
        long ttl = Duration.between(LocalDateTime.now(), logistic.getSignTime()).toMillis();
        // 健壮性：如果计算结果为负数，说明时间已过，立即触发
        ttl = Math.max(ttl, 1000L);

        sendDelayMessage(logistic.getLogisticId(), ttl);
    }

    /**
     * 发送自动签收消息（待签收 -> 已签收）
     */
    public void sendAutoSignMessage(Logistic logistic) {
        // 只有状态 1（待签收）才能进入自动签收倒计时
        if (logistic == null || logistic.getStatus() != 1) return;

        log.info("[物流消息] 发送自动签收倒计时，物流ID：{}", logistic.getLogisticId());

        if (logistic.getAutoSignTime() == null) {
            log.error("自动签收时间不能为空");
            return;
        }

        long ttl = Duration.between(LocalDateTime.now(), logistic.getAutoSignTime()).toMillis();
        ttl = Math.max(ttl, 1000L);

        sendDelayMessage(logistic.getLogisticId(), ttl);
    }

    private void sendDelayMessage(String logisticId, long ttl) {
        rabbitTemplate.convertAndSend(
                "logistic.ttl.exchange",
                "logistic.ttl.key",
                logisticId,
                message -> {
                    message.getMessageProperties().setExpiration(String.valueOf(ttl));
                    return message;
                }
        );
    }
}