package com.mall.demo.mq.producer;


import com.mall.demo.module.product.entity.SKU;
import com.mall.demo.module.product.entity.SPU;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j  // 添加这个注解
@Component  // 让Spring管理这个Bean
@RequiredArgsConstructor
public class ProductProducer {

    private final RabbitTemplate rabbitTemplate;


    // 发布商品缓存spu
    public void sendPublishSpu(SPU spu) {

        rabbitTemplate.convertAndSend(
                "product.exchange",
                "product.publish.spu.key",
                spu);
    }


    // 发布商品缓存sku
    public void sendPublishSku(SKU sku) {
        rabbitTemplate.convertAndSend(
                "product.exchange",
                "product.publish.sku.key",
                sku);
    }



    // 删除商品缓存spu
    public void sendRevokeSpu(String spuId) {
        rabbitTemplate.convertAndSend(
                "product.exchange",
                "product.revoke.spu.key",
                spuId);
    }

    // 删除商品缓存sku
    public void sendRevokeSku(String skuId) {
        rabbitTemplate.convertAndSend(
                "product.exchange",
                "product.revoke.sku.key",
                skuId);
    }


    // 删除商品数据库数据spu
    public void sendDeleteSpu(String spuId) {
        rabbitTemplate.convertAndSend(
                "product.exchange",
                "product.delete.spu.key",
                spuId);
    }


    // 删除商品数据库数据sku
    public void sendDeleteSku(String skuId) {
        rabbitTemplate.convertAndSend(
                "product.exchange",
                "product.delete.sku.key",
                skuId);
    }
}
