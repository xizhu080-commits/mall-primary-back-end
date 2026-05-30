/*
package com.mall.demo.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.demo.module.order.entity.MyOrder;
import com.mall.demo.module.order.entity.OrderItem;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.OrderItemMapper;
import com.mall.demo.module.order.mapper.OrderMapper;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.module.order.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StockService stockService;
    private final SuborderMapper suborderMapper;

    // 订单处理允许的最大系统误差（如300秒，即5分钟）
    private static final long ORDER_EXPIRE_TOLERANCE = 300000L;

    @RabbitListener(queues = "order.dlx.queue")
    public void handleOrderTimeout(String orderId ) {


        */
/**
         * 订单状态: 1->待付款；2->待发货；3->已发货；4->已完成；5->退款中; 6->退款成功;0->已关闭
         *//*




        // 1. 查询数据库，获取最新状态
        MyOrder myOrder = orderMapper.selectById(orderId);
        LambdaQueryWrapper<Suborder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Suborder::getOrderId, orderId);
        List<Suborder> suborderList = suborderMapper.selectList(queryWrapper);

        // 2. 状态校验：数据库已删除或状态不是“待支付(0)”则直接退出
        if (myOrder == null || myOrder.getStatus() != 0) {
            return;
        }

        // 3. 时效性校验（核心）：
        // 如果当前时间已经比该订单的 expire_time 晚了 5 分钟以上
        // 说明这是重启项目导致的旧消息推送，不应该打印 info 日志
        if (myOrder.getExpireTime() != null) {
            Duration duration = Duration.between(myOrder.getExpireTime(), LocalDateTime.now());
            if (duration.toMillis() > ORDER_EXPIRE_TOLERANCE) {
                // 静默执行关单（为了保证最终一致性），但不打印超时触发日志
                orderMapper.closeOrderIfUnpaid(orderId);
                return;
            }
        }

        // 4. 只有新鲜的实时超时才进行完整输出
        log.info("[消费者中心] 检测到订单实时超时，开始处理：{}", orderId);

        int result = orderMapper.closeOrderIfUnpaid(orderId);
        if (result == 0) {
            return;
        }

        // 5. 状态同步与回滚库存
        myOrder.setStatus(0);
        orderMapper.updateById(myOrder);
         // 若 closeOrderIfUnpaid 已改状态则此处可选


        for (Suborder suborder : suborderList) {
            suborder.setStatus(0);
            suborderMapper.updateById(suborder);
        }



        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                stockService.addStock(item.getSkuId(), item.getQuantity());
            }
            log.info("订单 {} 实时超时处理成功，库存已回滚", orderId);
        }
    }
}*/




package com.mall.demo.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.module.order.entity.MyOrder;
import com.mall.demo.module.order.entity.OrderItem;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.OrderItemMapper;
import com.mall.demo.module.order.mapper.OrderMapper;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.module.order.service.StockService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StockService stockService;
    private final SuborderMapper suborderMapper;

    private static final long ORDER_EXPIRE_TOLERANCE = 300000L;
    private static final long OLD_MESSAGE_THRESHOLD = 3600000L;

    @RabbitListener(queues = "order.dlx.queue")
    public void handleOrderTimeout(String orderId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            processOrderTimeout(orderId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理订单超时失败: {}, 错误: {}", orderId, e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    private void processOrderTimeout(String orderId) {
        MyOrder myOrder = orderMapper.selectById(orderId);
        if (myOrder == null) {
            log.debug("订单不存在: {}", orderId);
            return;
        }

        if (myOrder.getStatus() != 0) {
            log.debug("订单状态不是待支付: {}, 状态: {}", orderId, myOrder.getStatus());
            return;
        }

        if (myOrder.getExpireTime() != null) {
            Duration duration = Duration.between(myOrder.getExpireTime(), LocalDateTime.now());
            if (duration.toMillis() > ORDER_EXPIRE_TOLERANCE) {
                if (duration.toMillis() > OLD_MESSAGE_THRESHOLD) {
                    log.warn("消息过期太久，跳过处理: {}", orderId);
                    return;
                }
                orderMapper.closeOrderIfUnpaid(orderId);
                return;
            }
        }

        log.info("[消费者中心] 检测到订单实时超时，开始处理：{}", orderId);

        int result = orderMapper.closeOrderIfUnpaid(orderId);
        if (result == 0) {
            return;
        }

        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        if (items != null && !items.isEmpty()) {
            items.forEach(item -> stockService.addStock(item.getSkuId(), item.getQuantity()));
            log.info("订单 {} 实时超时处理成功，库存已回滚", orderId);
        }
    }
}