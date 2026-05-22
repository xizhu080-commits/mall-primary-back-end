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
    /**
     * 私有校验方法：判断业务时间是否已经过期
     */
    private boolean isExpired(LocalDateTime updateTime) {
        if (updateTime == null) return false;
        Duration duration = Duration.between(updateTime, LocalDateTime.now());
        return duration.toMillis() > TIME_THRESHOLD;
    }
}
