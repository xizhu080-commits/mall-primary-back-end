/*
package com.mall.demo.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.module.coupon.entity.CouponTemplate;
import com.mall.demo.module.coupon.entity.CouponUser;
import com.mall.demo.module.coupon.mapper.CouponTemplateMapper;
import com.mall.demo.module.coupon.mapper.CouponUserMapper;
import com.mall.demo.module.product.entity.SKU;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j  // 添加这个注解
@Component  // 让Spring管理这个Bean
@RequiredArgsConstructor
public class CouponConsumer {


    private final RedisService redisService;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponUserMapper couponUserMapper;



    //发送商品sku缓存
    @RabbitListener(queues = "coupon.couponTemplate.publish.queue")
    public void handleSkuCache(CouponTemplate couponTemplate) {

        log.info("【缓存中心】正在将优惠券模板 {} 存入缓存...", couponTemplate.getTemplateId());
        String json = JSON.toJSONString(couponTemplate);
        // 存入缓存
        redisService.set("couponTemplate:" + couponTemplate.getTemplateId(), json,  60);
        log.info("【缓存中心】优惠券模板 {} 存入缓存成功！", couponTemplate.getTemplateId());


    }

    */
/*
    * 定时发布优惠卷
    * *//*

    @RabbitListener(queues = "coupon.couponTemplate.start.dlx.queue")
    public void handleStartCouponTemplate( CouponTemplate couponTemplate ) {

        if (couponTemplate == null ) {
            return;
        }
        Integer status = couponTemplate.getStatus();
        if (status != null && status == 1) {
            log.debug("【优惠券中心】优惠券模板 {} 已发布，跳过处理", couponTemplate.getTemplateId());
            return;
        }
        if (couponTemplate.getStartTime() == null) {
            log.warn("【优惠券中心】优惠券模板 {} 的开始时间为空，跳过处理", couponTemplate.getTemplateId());
            return;
        }

        long ttl = Duration.between(LocalDateTime.now(), couponTemplate.getStartTime()).toMillis();
        if (ttl <= 0) {
            couponTemplate.setStatus(1);
            couponTemplateMapper.updateById(couponTemplate);
            log.info("【优惠券中心】优惠券模板 {} 已发布！", couponTemplate.getTemplateId());
        }

    }

    */
/*
    * 定时下架优惠卷
    * *//*

    @RabbitListener(queues = "coupon.couponTemplate.delete.dlx.queue")
    public void handleDeleteCouponTemplate(CouponTemplate couponTemplate) {

         if (couponTemplate == null ) {
            return;
        }
        long ttl = Duration.between(LocalDateTime.now(), couponTemplate.getEndTime()).toMillis();
        if (ttl <= 0) {
            couponTemplateMapper.deleteById(couponTemplate);
        }

    }





    */
/*
     * 有效时间结束,更新状态
     * *//*

    @RabbitListener(queues = "coupon.couponUser.update.dlx.queue")
    public void handleUpdateCouponUser(CouponUser couponUser) {  // 改为 CouponUser
        if (couponUser == null) {
            return;
        }
        if (couponUser.getStatus() == 2) {
            return;
        }
        long ttl = Duration.between(LocalDateTime.now(), couponUser.getEndTime()).toMillis();
        if (ttl <= 0) {
            couponUser.setStatus(2);
            couponUserMapper.updateById(couponUser);
            log.info("【优惠券中心】优惠券用户 {} 已过期！", couponUser.getUserId());
        }
    }




    */
/*
     * 过期时间结束,删除数据库记录
     * *//*


    @RabbitListener(queues = "coupon.couponUser.delete.dlx.queue")
    public void handleDeleteCouponUser(CouponUser couponUser) {

        if (couponUser == null ) {
            return;
        }

        long ttl = Duration.between(LocalDateTime.now(), couponUser.getExpireTime()).toMillis();
        if (ttl <= 0) {
            couponUserMapper.deleteById(couponUser);
        }
    }
}
*/













package com.mall.demo.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.module.coupon.entity.CouponTemplate;
import com.mall.demo.module.coupon.entity.CouponUser;
import com.mall.demo.module.coupon.mapper.CouponTemplateMapper;
import com.mall.demo.module.coupon.mapper.CouponUserMapper;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponConsumer {

    private final RedisService redisService;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponUserMapper couponUserMapper;
    private static final long OLD_MESSAGE_THRESHOLD = 3600000L;

    @RabbitListener(queues = "coupon.couponTemplate.publish.queue")
    public void handleSkuCache(CouponTemplate couponTemplate, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (couponTemplate == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("【缓存中心】正在将优惠券模板 {} 存入缓存...", couponTemplate.getTemplateId());
            String json = JSON.toJSONString(couponTemplate);
            redisService.set("couponTemplate:" + couponTemplate.getTemplateId(), json, 60);
            log.info("【缓存中心】优惠券模板 {} 存入缓存成功！", couponTemplate.getTemplateId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理优惠券缓存失败: {}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(queues = "coupon.couponTemplate.start.dlx.queue")
    public void handleStartCouponTemplate(CouponTemplate couponTemplate, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (couponTemplate == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (couponTemplate.getStatus() != null && couponTemplate.getStatus() == 1) {
                log.debug("【优惠券中心】优惠券模板 {} 已发布，跳过处理", couponTemplate.getTemplateId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (couponTemplate.getStartTime() == null) {
                log.warn("【优惠券中心】优惠券模板 {} 的开始时间为空，跳过处理", couponTemplate.getTemplateId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            long ttl = Duration.between(LocalDateTime.now(), couponTemplate.getStartTime()).toMillis();
            if (ttl <= 0) {
                couponTemplate.setStatus(1);
                couponTemplateMapper.updateById(couponTemplate);
                log.info("【优惠券中心】优惠券模板 {} 已发布！", couponTemplate.getTemplateId());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理优惠券发布失败: {}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(queues = "coupon.couponTemplate.delete.dlx.queue")
    public void handleDeleteCouponTemplate(CouponTemplate couponTemplate, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (couponTemplate == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            long ttl = Duration.between(LocalDateTime.now(), couponTemplate.getEndTime()).toMillis();
            if (ttl <= 0) {
                if (Math.abs(ttl) > OLD_MESSAGE_THRESHOLD) {
                    log.warn("优惠券删除消息过期太久，跳过处理: {}", couponTemplate.getTemplateId());
                    channel.basicAck(deliveryTag, false);
                    return;
                }
                couponTemplateMapper.deleteById(couponTemplate);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理优惠券删除失败: {}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(queues = "coupon.couponUser.update.dlx.queue")
    public void handleUpdateCouponUser(CouponUser couponUser, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (couponUser == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (couponUser.getStatus() == 2) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            long ttl = Duration.between(LocalDateTime.now(), couponUser.getEndTime()).toMillis();
            if (ttl <= 0) {
                if (Math.abs(ttl) > OLD_MESSAGE_THRESHOLD) {
                    log.warn("优惠券用户更新消息过期太久，跳过处理: {}", couponUser.getUserId());
                    channel.basicAck(deliveryTag, false);
                    return;
                }
                couponUser.setStatus(2);
                couponUserMapper.updateById(couponUser);
                log.info("【优惠券中心】优惠券用户 {} 已过期！", couponUser.getUserId());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理优惠券用户更新失败: {}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(queues = "coupon.couponUser.delete.dlx.queue")
    public void handleDeleteCouponUser(CouponUser couponUser, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (couponUser == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            long ttl = Duration.between(LocalDateTime.now(), couponUser.getExpireTime()).toMillis();
            if (ttl <= 0) {
                if (Math.abs(ttl) > OLD_MESSAGE_THRESHOLD) {
                    log.warn("优惠券用户删除消息过期太久，跳过处理: {}", couponUser.getUserId());
                    channel.basicAck(deliveryTag, false);
                    return;
                }
                couponUserMapper.deleteById(couponUser);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理优惠券用户删除失败: {}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }
}