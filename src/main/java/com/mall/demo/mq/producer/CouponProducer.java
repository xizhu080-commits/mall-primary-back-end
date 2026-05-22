package com.mall.demo.mq.producer;

import com.mall.demo.module.coupon.entity.CouponTemplate;
import com.mall.demo.module.coupon.entity.CouponUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j  // 添加这个注解
@Component  // 让Spring管理这个Bean
@RequiredArgsConstructor
public class CouponProducer {

    private final RabbitTemplate rabbitTemplate;


    /*
     * 发送优惠卷消息到队列
     * */
    public void sendPublishCouponTemplate(CouponTemplate couponTemplate) {

        rabbitTemplate.convertAndSend(
                "coupon.exchange",
                "coupon.couponTemplate.publish.key",
                couponTemplate);
    }


    /*
     * 过期删除
     * */
    public void sendDeleteCouponTemplate(CouponTemplate couponTemplate) {

        long ttl = Duration.between(LocalDateTime.now(), couponTemplate.getEndTime()).toMillis();

        if (ttl <= 0) {
            log.warn("优惠券模板已结束，无需设置TTL删除，templateId: {}", couponTemplate.getTemplateId());
            return;
        }
        rabbitTemplate.convertAndSend(
                "coupon.ttl.exchange",
                "coupon.couponTemplate.delete.ttl.key",
                couponTemplate,
                message -> {
                    message.getMessageProperties().setExpiration(String.valueOf(ttl));
                    return message;
                }
        );
    }

    /*
     * 定时发布优惠卷
     * */
    public void sendStartCouponTemplate(CouponTemplate couponTemplate) {
        long ttl = Duration.between(LocalDateTime.now(), couponTemplate.getStartTime()).toMillis();
        if (ttl <= 0) {
            log.warn("优惠券模板已开始，无需设置TTL延迟，templateId: {}", couponTemplate.getTemplateId());
            return;
        }
        rabbitTemplate.convertAndSend(
                "coupon.ttl.exchange",
                "coupon.couponTemplate.start.ttl.key",
                couponTemplate,
                message -> {
                    message.getMessageProperties().setExpiration(String.valueOf(ttl));
                    return message;
                }
        );
    }


    /*
     * 有效时间结束,更新状态
     * */
    public void sendCouponUser(CouponUser couponUser) {
        long ttl = Duration.between(LocalDateTime.now(), couponUser.getEndTime()).toMillis();
        if (ttl <= 0) {
            log.warn("优惠券已过期，无需设置TTL，couponId: {}", couponUser.getCouponId());
            return;
        }
        rabbitTemplate.convertAndSend(
                "coupon.ttl.exchange",
                "coupon.couponUser.status.ttl.key",
                couponUser,
                message -> {
                    message.getMessageProperties().setExpiration(String.valueOf(ttl));
                    return message;
                }
        );
    }

    /*
     * 过期时间结束,删除数据库记录
     * */
    public void sendDeleteCouponUser(CouponUser couponUser) {
        long ttl = Duration.between(LocalDateTime.now(), couponUser.getExpireTime()).toMillis();
        if (ttl <= 0) {
            log.warn("优惠券已过有效期，无需设置TTL删除，couponId: {}", couponUser.getCouponId());
            return;
        }
        rabbitTemplate.convertAndSend(
                "coupon.ttl.exchange",
                "coupon.couponUser.delete.ttl.key",
                couponUser,
                message -> {
                    message.getMessageProperties().setExpiration(String.valueOf(ttl));
                    return message;
                }
        );

    }
}
