package com.mall.demo.common.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // ================== 用户模块 ==================

    //交换机
    public static final String USER_EXCHANGE = "user-exchange";
    //注册业务---------
    public static final String USER_REGISTER_QUEUE = "user.register.queue";
    public static final String USER_REGISTER_KEY = "user.register.key";
    //修改业务---------
    public static final String USER_UPDATE_QUEUE = "user.update.queue";
    public static final String USER_UPDATE_KEY = "user.update.key";


    @Bean
    public DirectExchange userExchange() {
        return new DirectExchange(USER_EXCHANGE);
    }

    //注册业务
    @Bean
    public Queue userQueue() {
        return new Queue(USER_REGISTER_QUEUE);
    }

    @Bean
    public Binding userBinding() {
        return BindingBuilder.bind(userQueue())
                .to(userExchange())
                .with(USER_REGISTER_KEY);
    }

    //修改业务
    @Bean
    public Queue userUpdateQueue() {
        return new Queue(USER_UPDATE_QUEUE);
    }

    @Bean
    public Binding userUpdateBinding() {
        return BindingBuilder.bind(userUpdateQueue())
                .to(userExchange())
                .with(USER_UPDATE_KEY);
    }


    // ================== 商品模块 ==================
    //交换机
    public static final String PRODUCT_EXCHANGE = "product.exchange";
    //发布商品缓存
    public static final String PRODUCT_PUBLISH_SPU_QUEUE = "product.publish.spu.queue";
    public static final String PRODUCT_PUBLISH_SPU_KEY = "product.publish.spu.key";
    public static final String PRODUCT_PUBLISH_SKU_QUEUE = "product.publish.sku.queue";
    public static final String PRODUCT_PUBLISH_SKU_KEY = "product.publish.sku.key";
    //删除商品缓存
    public static final String PRODUCT_REVOKE_SPU_QUEUE = "product.revoke.spu.queue";
    public static final String PRODUCT_REVOKE_SPU_KEY = "product.revoke.spu.key";
    public static final String PRODUCT_REVOKE_SKU_QUEUE = "product.revoke.sku.queue";
    public static final String PRODUCT_REVOKE_SKU_KEY = "product.revoke.sku.key";
    //删除商品数据库数据
    public static final String PRODUCT_DELETE_SPU_QUEUE = "product.delete.spu.queue";
    public static final String PRODUCT_DELETE_SPU_KEY = "product.delete.spu.key";
    //删除商品数据库数据
    public static final String PRODUCT_DELETE_SKU_QUEUE = "product.delete.sku.queue";
    public static final String PRODUCT_DELETE_SKU_KEY = "product.delete.sku.key";

    @Bean
    public DirectExchange productExchange() {
        return new DirectExchange(PRODUCT_EXCHANGE);
    }

    //spu队列
    @Bean
    public Queue productPublishSpuQueue() {
        return new Queue(PRODUCT_PUBLISH_SPU_QUEUE);
    }

    @Bean
    public Binding productPublishSpuBinding() {
        return BindingBuilder.bind(productPublishSpuQueue())
                .to(productExchange())
                .with(PRODUCT_PUBLISH_SPU_KEY);
    }

    @Bean
    public Queue productRevokeSpuQueue() {
        return new Queue(PRODUCT_REVOKE_SPU_QUEUE);
    }

    @Bean
    public Binding productRevokeSpuBinding() {
        return BindingBuilder.bind(productRevokeSpuQueue())
                .to(productExchange())
                .with(PRODUCT_REVOKE_SPU_KEY);
    }

    @Bean
    public Queue productDeleteSpuQueue() {
        return new Queue(PRODUCT_DELETE_SPU_QUEUE);
    }

    @Bean
    public Binding productDeleteSpuBinding() {
        return BindingBuilder.bind(productDeleteSpuQueue())
                .to(productExchange())
                .with(PRODUCT_DELETE_SPU_KEY);
    }


    //sku队列
    @Bean
    public Queue productPublishSkuQueue() {
        return new Queue(PRODUCT_PUBLISH_SKU_QUEUE);
    }

    @Bean
    public Binding productPublishSkuBinding() {
        return BindingBuilder.bind(productPublishSkuQueue())
                .to(productExchange())
                .with(PRODUCT_PUBLISH_SKU_KEY);
    }

    @Bean
    public Queue productRevokeSkuQueue() {
        return new Queue(PRODUCT_REVOKE_SKU_QUEUE);
    }

    @Bean
    public Binding productRevokeSkuBinding() {
        return BindingBuilder.bind(productRevokeSkuQueue())
                .to(productExchange())
                .with(PRODUCT_REVOKE_SKU_KEY);
    }

    @Bean
    public Queue productDeleteSkuQueue() {
        return new Queue(PRODUCT_DELETE_SKU_QUEUE);
    }

    @Bean
    public Binding productDeleteSkuBinding() {
        return BindingBuilder.bind(productDeleteSkuQueue())
                .to(productExchange())
                .with(PRODUCT_DELETE_SKU_KEY);
    }


    // ================== 订单模块 ==================

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_QUEUE = "order.create.queue";
    public static final String ORDER_ROUTING_KEY = "order.create";
    // TTL队列
    public static final String ORDER_TTL_QUEUE = "order.ttl.queue";
    public static final String ORDER_TTL_EXCHANGE = "order.ttl.exchange";
    public static final String ORDER_TTL_ROUTING_KEY = "order.ttl.key";

    // 死信队列
    public static final String ORDER_DLX_QUEUE = "order.dlx.queue";
    public static final String ORDER_DLX_EXCHANGE = "order.dlx.exchange";
    public static final String ORDER_DLX_ROUTING_KEY = "order.dlx.key";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE);
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with(ORDER_ROUTING_KEY);
    }

    //TTL
    @Bean
    public DirectExchange orderTtlExchange() {
        return new DirectExchange(ORDER_TTL_EXCHANGE);
    }

    @Bean
    public Queue orderTtlQueue() {

        Map<String, Object> args = new HashMap<>();

        // ❗ 指定死信交换机
        args.put("x-dead-letter-exchange", ORDER_DLX_EXCHANGE);

        // ❗ 指定死信routingKey
        args.put("x-dead-letter-routing-key", ORDER_DLX_ROUTING_KEY);

        return new Queue(ORDER_TTL_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding orderTtlBinding() {
        return BindingBuilder.bind(orderTtlQueue())
                .to(orderTtlExchange())
                .with(ORDER_TTL_ROUTING_KEY);
    }


    //死信
    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(ORDER_DLX_EXCHANGE);
    }

    @Bean
    public Queue orderDlxQueue() {
        return new Queue(ORDER_DLX_QUEUE);
    }

    @Bean
    public Binding orderDlxBinding() {
        return BindingBuilder.bind(orderDlxQueue())
                .to(orderDlxExchange())
                .with(ORDER_DLX_ROUTING_KEY);
    }


    // ================== 优惠券模块 ==================

    // 普通业务交换机与队列（用于缓存同步等实时任务）
    public static final String COUPON_EXCHANGE = "coupon.exchange";
    public static final String COUPON_PUBLISH_QUEUE = "coupon.couponTemplate.publish.queue";
    public static final String COUPON_PUBLISH_KEY = "coupon.couponTemplate.publish.key";

    // --- TTL 延迟处理部分 ---
    public static final String COUPON_TTL_EXCHANGE = "coupon.ttl.exchange";

    // 1. 模板开始（上架）
    public static final String COUPON_START_TTL_QUEUE = "coupon.couponTemplate.start.ttl.queue";
    public static final String COUPON_START_TTL_KEY = "coupon.couponTemplate.start.ttl.key";
    public static final String COUPON_START_DLX_QUEUE = "coupon.couponTemplate.start.dlx.queue";
    public static final String COUPON_START_DLX_KEY = "coupon.couponTemplate.start.dlx.key";

    // 2. 模板结束（下架/删除）
    public static final String COUPON_DELETE_TTL_QUEUE = "coupon.couponTemplate.delete.ttl.queue";
    public static final String COUPON_DELETE_TTL_KEY = "coupon.couponTemplate.delete.ttl.key";
    public static final String COUPON_DELETE_DLX_QUEUE = "coupon.couponTemplate.delete.dlx.queue";
    public static final String COUPON_DELETE_DLX_KEY = "coupon.couponTemplate.delete.dlx.key";

    // 3. 用户优惠券状态更新（过期变已使用/失效）
    public static final String COUPON_USER_STATUS_TTL_QUEUE = "coupon.couponUser.status.ttl.queue";
    public static final String COUPON_USER_STATUS_TTL_KEY = "coupon.couponUser.status.ttl.key";
    public static final String COUPON_USER_STATUS_DLX_QUEUE = "coupon.couponUser.update.dlx.queue";
    public static final String COUPON_USER_STATUS_DLX_KEY = "coupon.couponUser.update.dlx.key";

    // 4. 用户优惠券物理删除
    public static final String COUPON_USER_DELETE_TTL_QUEUE = "coupon.couponUser.delete.ttl.queue";
    public static final String COUPON_USER_DELETE_TTL_KEY = "coupon.couponUser.delete.ttl.key";
    public static final String COUPON_USER_DELETE_DLX_QUEUE = "coupon.couponUser.delete.dlx.queue";
    public static final String COUPON_USER_DELETE_DLX_KEY = "coupon.couponUser.delete.dlx.key";

    // 死信交换机（所有过期的优惠券消息都会传到这里，由它分发到具体的死信队列）
    public static final String COUPON_DLX_EXCHANGE = "coupon.dlx.exchange";

    @Bean
    public DirectExchange couponExchange() {
        return new DirectExchange(COUPON_EXCHANGE);
    }

    @Bean
    public DirectExchange couponTtlExchange() {
        return new DirectExchange(COUPON_TTL_EXCHANGE);
    }

    @Bean
    public DirectExchange couponDlxExchange() {
        return new DirectExchange(COUPON_DLX_EXCHANGE);
    }

    // --- 队列定义与死信配置 ---

    // 实时发布缓存队列
    @Bean
    public Queue couponPublishQueue() {
        return new Queue(COUPON_PUBLISH_QUEUE);
    }

    @Bean
    public Binding couponPublishBinding() {
        return BindingBuilder.bind(couponPublishQueue()).to(couponExchange()).with(COUPON_PUBLISH_KEY);
    }

    // 通用的 TTL 队列构建方法（设置死信转发）
    private Queue createTtlQueue(String name, String dlxRoutingKey) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", COUPON_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", dlxRoutingKey);
        return new Queue(name, true, false, false, args);
    }

    // --- 1. 模板开始逻辑 ---
    @Bean
    public Queue startTtlQueue() {
        return createTtlQueue(COUPON_START_TTL_QUEUE, COUPON_START_DLX_KEY);
    }

    @Bean
    public Binding startTtlBinding() {
        return BindingBuilder.bind(startTtlQueue()).to(couponTtlExchange()).with(COUPON_START_TTL_KEY);
    }

    @Bean
    public Queue startDlxQueue() {
        return new Queue(COUPON_START_DLX_QUEUE);
    }

    @Bean
    public Binding startDlxBinding() {
        return BindingBuilder.bind(startDlxQueue()).to(couponDlxExchange()).with(COUPON_START_DLX_KEY);
    }

    // --- 2. 模板删除逻辑 ---
    @Bean
    public Queue deleteTtlQueue() {
        return createTtlQueue(COUPON_DELETE_TTL_QUEUE, COUPON_DELETE_DLX_KEY);
    }

    @Bean
    public Binding deleteTtlBinding() {
        return BindingBuilder.bind(deleteTtlQueue()).to(couponTtlExchange()).with(COUPON_DELETE_TTL_KEY);
    }

    @Bean
    public Queue deleteDlxQueue() {
        return new Queue(COUPON_DELETE_DLX_QUEUE);
    }

    @Bean
    public Binding deleteDlxBinding() {
        return BindingBuilder.bind(deleteDlxQueue()).to(couponDlxExchange()).with(COUPON_DELETE_DLX_KEY);
    }

    // --- 3. 用户券状态更新逻辑 ---
    @Bean
    public Queue userStatusTtlQueue() {
        return createTtlQueue(COUPON_USER_STATUS_TTL_QUEUE, COUPON_USER_STATUS_DLX_KEY);
    }

    @Bean
    public Binding userStatusTtlBinding() {
        return BindingBuilder.bind(userStatusTtlQueue()).to(couponTtlExchange()).with(COUPON_USER_STATUS_TTL_KEY);
    }

    @Bean
    public Queue userStatusDlxQueue() {
        return new Queue(COUPON_USER_STATUS_DLX_QUEUE);
    }

    @Bean
    public Binding userStatusDlxBinding() {
        return BindingBuilder.bind(userStatusDlxQueue()).to(couponDlxExchange()).with(COUPON_USER_STATUS_DLX_KEY);
    }

    // --- 4. 用户券删除逻辑 ---
    @Bean
    public Queue userDeleteTtlQueue() {
        return createTtlQueue(COUPON_USER_DELETE_TTL_QUEUE, COUPON_USER_DELETE_DLX_KEY);
    }

    @Bean
    public Binding userDeleteTtlBinding() {
        return BindingBuilder.bind(userDeleteTtlQueue()).to(couponTtlExchange()).with(COUPON_USER_DELETE_TTL_KEY);
    }

    @Bean
    public Queue userDeleteDlxQueue() {
        return new Queue(COUPON_USER_DELETE_DLX_QUEUE);
    }

    @Bean
    public Binding userDeleteDlxBinding() {
        return BindingBuilder.bind(userDeleteDlxQueue()).to(couponDlxExchange()).with(COUPON_USER_DELETE_DLX_KEY);
    }


    // 退款通知队列
    public static final String REFUND_QUEUE = "refund.notify.queue";
    public static final String REFUND_EXCHANGE = "refund.notify.exchange";
    public static final String REFUND_ROUTING_KEY = "refund.notify";

    // 商家处理结果队列
    public static final String HANDLE_RESULT_QUEUE = "refund.handle.queue";
    public static final String HANDLE_RESULT_EXCHANGE = "refund.handle.exchange";
    public static final String HANDLE_RESULT_ROUTING_KEY = "refund.handle";

    @Bean
    public Queue refundNotifyQueue() {
        // 持久化队列，服务器重启消息不丢
        return QueueBuilder.durable(REFUND_QUEUE).build();
    }

    @Bean
    public DirectExchange refundNotifyExchange() {
        return ExchangeBuilder.directExchange(REFUND_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding refundNotifyBinding() {
        return BindingBuilder.bind(refundNotifyQueue())
                .to(refundNotifyExchange())
                .with(REFUND_ROUTING_KEY);
    }

    @Bean
    public Queue refundHandleQueue() {
        return QueueBuilder.durable(HANDLE_RESULT_QUEUE).build();
    }

    @Bean
    public DirectExchange refundHandleExchange() {
        return ExchangeBuilder.directExchange(HANDLE_RESULT_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding refundHandleBinding() {
        return BindingBuilder.bind(refundHandleQueue())
                .to(refundHandleExchange())
                .with(HANDLE_RESULT_ROUTING_KEY);
    }














        // --- 1. 物流通知相关 (普通队列) ---
        public static final String LOGISTIC_NOTIFY_EXCHANGE = "logistic.notify.exchange";
        public static final String LOGISTIC_NOTIFY_QUEUE = "logistic.notify.queue";
        public static final String LOGISTIC_NOTIFY_ROUTING_KEY = "logistic.notify.routing";

        // --- 2. 自动签收相关 (死信延迟队列) ---
        // 业务队列：消息先发到这里，不被消费，等待超时
        public static final String LOGISTIC_TTL_EXCHANGE = "logistic.ttl.exchange";
        public static final String LOGISTIC_TTL_QUEUE = "logistic.ttl.queue";
        public static final String LOGISTIC_TTL_ROUTING_KEY = "logistic.ttl.routing";

        // 死信交换机：消息超时后转入这里，由消费者处理自动签收逻辑
        public static final String LOGISTIC_DLX_EXCHANGE = "logistic.dlx.exchange";
        public static final String LOGISTIC_DLX_QUEUE = "logistic.dlx.queue";
        public static final String LOGISTIC_DLX_ROUTING_KEY = "logistic.dlx.routing";

        /**
         * 物流通知交换机
         */
        @Bean
        public DirectExchange logisticNotifyExchange() {
            return new DirectExchange(LOGISTIC_NOTIFY_EXCHANGE);
        }

        @Bean
        public Queue logisticNotifyQueue() {
            return new Queue(LOGISTIC_NOTIFY_QUEUE);
        }

        @Bean
        public Binding logisticNotifyBinding() {
            return BindingBuilder.bind(logisticNotifyQueue()).to(logisticNotifyExchange()).with(LOGISTIC_NOTIFY_ROUTING_KEY);
        }

        /**
         * 自动签收死信队列配置
         * 原理：发送消息到 DelayQueue，设置 TTL（如7天），超时后自动路由到 CloseQueue
         */
        @Bean
        public Queue logisticDelayQueue() {
            Map<String, Object> args = new HashMap<>();
            // 消息过期后发送到的交换机
            args.put("x-dead-letter-exchange", LOGISTIC_DLX_EXCHANGE);
            // 消息过期后发送到的路由键
            args.put("x-dead-letter-routing-key", LOGISTIC_DLX_ROUTING_KEY);
            // 设置队列消息过期时间 (例如 7天: 7 * 24 * 60 * 60 * 1000)
            // 调试时可以设置短一点，比如 10秒
            args.put("x-message-ttl", 10000);
            return new Queue(LOGISTIC_TTL_QUEUE, true, false, false, args);
        }

        @Bean
        public DirectExchange logisticDelayExchange() {
            return new DirectExchange(LOGISTIC_TTL_EXCHANGE);
        }

        @Bean
        public Binding logisticDelayBinding() {
            return BindingBuilder.bind(logisticDelayQueue()).to(logisticDelayExchange()).with(LOGISTIC_TTL_ROUTING_KEY);
        }

        // --- 死信接收部分 ---
        @Bean
        public DirectExchange logisticCloseExchange() {
            return new DirectExchange(LOGISTIC_DLX_EXCHANGE);
        }

        @Bean
        public Queue logisticCloseQueue() {
            return new Queue(LOGISTIC_DLX_QUEUE);
        }

        @Bean
        public Binding logisticCloseBinding() {
            return BindingBuilder.bind(logisticCloseQueue()).to(logisticCloseExchange()).with(LOGISTIC_DLX_ROUTING_KEY);
        }




    // ==================== 支付通知配置 ====================
    // ==================== 3. 支付通知相关 (新增) ====================
    public static final String PAYMENT_NOTIFY_EXCHANGE = "payment.notify.exchange";
    public static final String PAYMENT_NOTIFY_QUEUE = "payment.notify.queue";
    public static final String PAYMENT_NOTIFY_ROUTING_KEY = "payment.notify.routing";


    /**
     * 支付通知交换机
     */
    @Bean
    public DirectExchange paymentNotifyExchange() {
        return new DirectExchange(PAYMENT_NOTIFY_EXCHANGE);
    }

    /**
     * 支付通知队列
     * 持久化队列，防止 RabbitMQ 重启后丢失
     */
    @Bean
    public Queue paymentNotifyQueue() {
        // durable: true 持久化，exclusive: false 非独占，autoDelete: false 不自动删除
        return new Queue(PAYMENT_NOTIFY_QUEUE, true, false, false);
    }

    /**
     * 支付通知绑定
     */
    @Bean
    public Binding paymentNotifyBinding() {
        return BindingBuilder.bind(paymentNotifyQueue())
                .to(paymentNotifyExchange())
                .with(PAYMENT_NOTIFY_ROUTING_KEY);
    }









    // ==================== 消息记录（用户/商家对话）配置 ====================

    // 消息记录交换机
    public static final String MESSAGE_RECORD_EXCHANGE = "message.record.exchange";

    // 用户消息队列
    public static final String USER_MESSAGE_QUEUE = "message.record.user.queue";
    public static final String USER_MESSAGE_KEY = "message.user";

    // 商家消息队列
    public static final String MERCHANT_MESSAGE_QUEUE = "message.record.merchant.queue";
    public static final String MERCHANT_MESSAGE_KEY = "message.merchant";

    /**
     * 消息记录交换机
     */
    @Bean
    public DirectExchange messageRecordExchange() {
        return new DirectExchange(MESSAGE_RECORD_EXCHANGE);
    }

    /**
     * 用户消息队列
     * 持久化队列，防止 RabbitMQ 重启后丢失
     */
    @Bean
    public Queue userMessageQueue() {
        return new Queue(USER_MESSAGE_QUEUE, true, false, false);
    }

    /**
     * 用户消息绑定
     */
    @Bean
    public Binding userMessageBinding() {
        return BindingBuilder.bind(userMessageQueue())
                .to(messageRecordExchange())
                .with(USER_MESSAGE_KEY);
    }

    /**
     * 商家消息队列
     * 持久化队列，防止 RabbitMQ 重启后丢失
     */
    @Bean
    public Queue merchantMessageQueue() {
        return new Queue(MERCHANT_MESSAGE_QUEUE, true, false, false);
    }

    /**
     * 商家消息绑定
     */
    @Bean
    public Binding merchantMessageBinding() {
        return BindingBuilder.bind(merchantMessageQueue())
                .to(messageRecordExchange())
                .with(MERCHANT_MESSAGE_KEY);
    }




}




