/*
package com.mall.demo.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.module.product.entity.Product_Category;
import com.mall.demo.module.product.entity.SKU;
import com.mall.demo.module.product.entity.SPU;
import com.mall.demo.module.product.mapper.SKUMapper;
import com.mall.demo.module.product.mapper.SPUMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProductConsumer {

    private final RedisService redisService;
    private final SKUMapper skuMapper;
    private final SPUMapper spuMapper;

private static final long TIME_THRESHOLD = 60000L;

    //发送商品sku缓存
    @RabbitListener(queues = "product.publish.sku.queue")
    public void handleSkuCache(SKU sku) {


        if (sku == null)  return;



        if (isExpired(sku.getUpdateTime())) {
            return;
        }


        log.info("【缓存中心】正在将商品SKU {} 存入缓存...", sku.getSkuId());
        String json = JSON.toJSONString(sku);
        // 存入缓存
        redisService.set("product:" + sku.getSkuId(), json,  60);
        log.info("【缓存中心】商品SKU {} 存入缓存成功！", sku.getSkuId());
    }
    //发送商品spu缓存
    @RabbitListener(queues = "product.publish.spu.queue")
    public void handleSpuCache(SPU spu) {

        if (spu == null)  return;

        if (isExpired(spu.getUpdateTime())) {
            return;
        }
    //    log.info("【缓存中心】正在将商品SPU {} 存入缓存...", spu.getSpuId());
        String json = JSON.toJSONString(spu);
        // 存入缓存
        redisService.set("product:" + spu.getSpuId(), json,  60);
      //  log.info("【缓存中心】商品SPU {} 存入缓存成功！", spu.getSpuId());
    }



    @RabbitListener(queues = "product.revoke.sku.queue")
    public void handlerRevokeSku(String skuId) {

        if (skuMapper.selectById(skuId) == null)    return;
    //    log.info("【缓存中心】正在删除商品SKU {} 的缓存...", skuId);
        // 删除缓存
        redisService.delete("product:" + skuId);
     //   log.info("【缓存中心】商品SKU {} 的缓存删除成功！", skuId);


    }
    @RabbitListener(queues = "product.revoke.spu.queue")
    public void handlerRevokeSpu(String spuId) {
        if (spuMapper.selectById(spuId) == null)

        redisService.delete("product:" + spuId);


    }


    //删除商品数据库数据
    @RabbitListener(queues = "product.delete.sku.queue")
    public void handleDeleteSku(String skuId) {
        if (skuMapper.selectById(skuId) == null)    return;
        skuMapper.deleteById(skuId);

    }
    @RabbitListener(queues = "product.delete.spu.queue")
    public void handleDeleteSpu(String spuId) {

        if (spuMapper.selectById(spuId) == null)    return;




        skuMapper.deleteById(spuId);
            }
    */
/**
     * 私有校验方法：判断业务时间是否已经过期
     *//*

    private boolean isExpired(LocalDateTime updateTime) {
        if (updateTime == null) return false;
        Duration duration = Duration.between(updateTime, LocalDateTime.now());
        return duration.toMillis() > TIME_THRESHOLD;
    }
}
*/














package com.mall.demo.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.module.product.entity.SKU;
import com.mall.demo.module.product.entity.SPU;
import com.mall.demo.module.product.mapper.SKUMapper;
import com.mall.demo.module.product.mapper.SPUMapper;
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
public class ProductConsumer {

    private final RedisService redisService;
    private final SKUMapper skuMapper;
    private final SPUMapper spuMapper;
    private static final long TIME_THRESHOLD = 60000L;
    private static final long OLD_MESSAGE_THRESHOLD = 3600000L;

    @RabbitListener(queues = "product.publish.sku.queue")
    public void handleSkuCache(SKU sku, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (sku == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (isExpired(sku.getUpdateTime())) {
                log.debug("商品SKU {} 数据已过期，跳过缓存", sku.getSkuId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("【缓存中心】正在将商品SKU {} 存入缓存...", sku.getSkuId());
            String json = JSON.toJSONString(sku);
            redisService.set("product:" + sku.getSkuId(), json, 60);
            log.info("【缓存中心】商品SKU {} 存入缓存成功！", sku.getSkuId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理商品SKU缓存失败: {}, 错误: {}", sku != null ? sku.getSkuId() : "null", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(queues = "product.publish.spu.queue")
    public void handleSpuCache(SPU spu, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (spu == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (isExpired(spu.getUpdateTime())) {
                log.debug("商品SPU {} 数据已过期，跳过缓存", spu.getSpuId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("【缓存中心】正在将商品SPU {} 存入缓存...", spu.getSpuId());
            String json = JSON.toJSONString(spu);
            redisService.set("product:" + spu.getSpuId(), json, 60);
            log.info("【缓存中心】商品SPU {} 存入缓存成功！", spu.getSpuId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理商品SPU缓存失败: {}, 错误: {}", spu != null ? spu.getSpuId() : "null", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(queues = "product.revoke.sku.queue")
    public void handlerRevokeSku(String skuId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (skuMapper.selectById(skuId) == null) {
                log.debug("商品SKU {} 不存在，跳过缓存删除", skuId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("【缓存中心】正在删除商品SKU {} 的缓存...", skuId);
            redisService.delete("product:" + skuId);
            log.info("【缓存中心】商品SKU {} 的缓存删除成功！", skuId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("删除商品SKU缓存失败: {}, 错误: {}", skuId, e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(queues = "product.revoke.spu.queue")
    public void handlerRevokeSpu(String spuId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (spuMapper.selectById(spuId) == null) {
                log.debug("商品SPU {} 不存在，跳过缓存删除", spuId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("【缓存中心】正在删除商品SPU {} 的缓存...", spuId);
            redisService.delete("product:" + spuId);
            log.info("【缓存中心】商品SPU {} 的缓存删除成功！", spuId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("删除商品SPU缓存失败: {}, 错误: {}", spuId, e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(queues = "product.delete.sku.queue")
    public void handleDeleteSku(String skuId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (skuMapper.selectById(skuId) == null) {
                log.debug("商品SKU {} 不存在，跳过删除", skuId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            skuMapper.deleteById(skuId);
            log.info("商品SKU {} 删除成功", skuId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("删除商品SKU失败: {}, 错误: {}", skuId, e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(queues = "product.delete.spu.queue")
    public void handleDeleteSpu(String spuId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (spuMapper.selectById(spuId) == null) {
                log.debug("商品SPU {} 不存在，跳过删除", spuId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            spuMapper.deleteById(spuId);
            log.info("商品SPU {} 删除成功", spuId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("删除商品SPU失败: {}, 错误: {}", spuId, e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败: {}", ioException.getMessage());
            }
        }
    }

    private boolean isExpired(LocalDateTime updateTime) {
        if (updateTime == null) return false;
        Duration duration = Duration.between(updateTime, LocalDateTime.now());
        return duration.toMillis() > TIME_THRESHOLD;
    }
}