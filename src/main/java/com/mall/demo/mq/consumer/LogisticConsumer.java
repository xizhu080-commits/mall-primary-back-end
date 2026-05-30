/*
package com.mall.demo.mq.consumer;

import com.mall.demo.module.logistic.entity.Logistic;
import com.mall.demo.module.logistic.mapper.LogisticMapper;
import com.mall.demo.mq.producer.LogisticProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticConsumer {

    private final LogisticMapper logisticMapper;
    private final LogisticProducer logisticProducer;

    @RabbitListener(queues = "logistic.dlx.queue")
    public void handleLogisticTimeout(String logisticId) {
        log.info("[物流消费者] 接收到延迟任务，物流ID：{}", logisticId);

        // 1. 获取最新实时数据
        Logistic logistic = logisticMapper.selectById(logisticId);
        if (logistic == null) {
            log.warn("物流不存在：{}", logisticId);
            return;
        }

        // 2. 逻辑分发
        // 情况 1：包裹派送中 -> 变更为“待签收”
        if (logistic.getStatus() == 0) {
            processToWaitSign(logistic);
        }
        // 情况 2：包裹待签收 -> 变更为“自动签收”
        else if (logistic.getStatus() == 1) {
            processToAutoSigned(logistic);
        }
    }

    private void processToWaitSign(Logistic logistic) {
        logistic.setStatus(1);
        // 待签收
        // 设置 7 天后自动签收
        LocalDateTime autoSignTime = LocalDateTime.now().plusDays(7);
        logistic.setAutoSignTime(autoSignTime);
        logistic.setUpdateTime(LocalDateTime.now());

        logisticMapper.updateById(logistic);

        // 再次触发延迟队列：开启自动签收倒计时
        logisticProducer.sendAutoSignMessage(logistic);
        log.info("物流 {} 已送达，进入待签收状态，开启7天自动签收计时", logistic.getLogisticId());
    }

    private void processToAutoSigned(Logistic logistic) {
        logistic.setStatus(2);
        // 已签收
        logistic.setConsigneeTime(LocalDateTime.now());
        // 实际签收时间
        logistic.setUpdateTime(LocalDateTime.now());

        logisticMapper.updateById(logistic);
        log.info("物流 {} 用户超时未确认，系统自动签收完成", logistic.getLogisticId());

        // TODO: 这里通常需要调用订单服务，将订单/子订单状态也更新为“已完成”
    }
}*/















package com.mall.demo.mq.consumer;

import com.mall.demo.module.logistic.entity.Logistic;
import com.mall.demo.module.logistic.mapper.LogisticMapper;
import com.mall.demo.mq.producer.LogisticProducer;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticConsumer {

    private final LogisticMapper logisticMapper;
    private final LogisticProducer logisticProducer;
    private static final long OLD_MESSAGE_THRESHOLD = 86400000L;

    @RabbitListener(queues = "logistic.dlx.queue")
    public void handleLogisticTimeout(String logisticId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("[物流消费者] 接收到延迟任务，物流ID：{}", logisticId);

            Logistic logistic = logisticMapper.selectById(logisticId);
            if (logistic == null) {
                log.warn("物流不存在：{}", logisticId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (logistic.getUpdateTime() != null) {
                Duration duration = Duration.between(logistic.getUpdateTime(), LocalDateTime.now());
                if (duration.toMillis() > OLD_MESSAGE_THRESHOLD) {
                    log.warn("物流消息过期太久，跳过处理: {}", logisticId);
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }

            if (logistic.getStatus() == 0) {
                processToWaitSign(logistic);
            } else if (logistic.getStatus() == 1) {
                processToAutoSigned(logistic);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理物流超时失败: {}, 错误: {}", logisticId, e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    private void processToWaitSign(Logistic logistic) {
        try {
            logistic.setStatus(1);
            LocalDateTime autoSignTime = LocalDateTime.now().plusDays(7);
            logistic.setAutoSignTime(autoSignTime);
            logistic.setUpdateTime(LocalDateTime.now());
            logisticMapper.updateById(logistic);
            logisticProducer.sendAutoSignMessage(logistic);
            log.info("物流 {} 已送达，进入待签收状态，开启7天自动签收计时", logistic.getLogisticId());
        } catch (Exception e) {
            log.error("处理物流待签收状态失败: {}, 错误: {}", logistic.getLogisticId(), e.getMessage(), e);
            throw e;
        }
    }

    private void processToAutoSigned(Logistic logistic) {
        try {
            logistic.setStatus(2);
            logistic.setConsigneeTime(LocalDateTime.now());
            logistic.setUpdateTime(LocalDateTime.now());
            logisticMapper.updateById(logistic);
            log.info("物流 {} 用户超时未确认，系统自动签收完成", logistic.getLogisticId());
        } catch (Exception e) {
            log.error("处理物流自动签收失败: {}, 错误: {}", logistic.getLogisticId(), e.getMessage(), e);
            throw e;
        }
    }
}