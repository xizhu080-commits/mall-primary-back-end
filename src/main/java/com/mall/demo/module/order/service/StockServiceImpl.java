package com.mall.demo.module.order.service;

import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.module.product.entity.SKU;
import com.mall.demo.module.product.mapper.SKUMapper;
import com.mall.demo.mq.producer.ProductProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> stockLuaScript;
    private final SKUMapper skuMapper;
    private final ProductProducer productProducer;

    /**
     * 扣库存（Redis Lua）
     */
    @Override
    public boolean deductStock(String skuId, Integer quantity) {
        String key = "skuId:" + skuId;
        Long result = stringRedisTemplate.execute(
                stockLuaScript,
                Collections.singletonList(key),
                quantity.toString()
        );
        if (result == null) {
            throw new BizException("库存服务异常");
        }
        switch (result.intValue()) {
            case 1:
                log.info("扣减成功, key={}, quantity={}", key, quantity);
                return true;
            case -1:
                // 缓存不存在，从数据库加载并重试扣减
                log.info("缓存不存在，从数据库加载库存, skuId={}", skuId);
                SKU sku = skuMapper.selectById(skuId);
                if (sku == null) {
                    throw new BizException(ErrorCodeEnum.PRO_NOT_FOUND.getCode(), ErrorCodeEnum.PRO_NOT_FOUND.getMessage());
                }
                if (sku.getStock() < quantity) {
                    throw new BizException(ErrorCodeEnum.STOCK_NOT_ENOUGH.getCode(), ErrorCodeEnum.STOCK_NOT_ENOUGH.getMessage());
                }
                // 同步数据库库存到 Redis
                stringRedisTemplate.opsForValue().set(key, String.valueOf(sku.getStock()));
                // 重新执行扣减（此时 Key 已存在且有值）
                Long retry = stringRedisTemplate.execute(
                        stockLuaScript,
                        Collections.singletonList(key),
                        quantity.toString()
                );
                if (retry == null || retry != 1) {
                    throw new BizException(ErrorCodeEnum.STOCK_NOT_ENOUGH.getCode(), ErrorCodeEnum.STOCK_NOT_ENOUGH.getMessage());
                }
                return true;
            case -2:
                log.warn("库存不足, key={}, quantity={}", key, quantity);
                throw new BizException(ErrorCodeEnum.STOCK_NOT_ENOUGH.getCode(), ErrorCodeEnum.STOCK_NOT_ENOUGH.getMessage());
            default:
                throw new BizException("库存扣减失败");
        }
    }
    /**
     * 回滚库存
     */
    @Override
    public void addStock(String skuId, Integer quantity) {

        String key = "skuId:" + skuId;

        stringRedisTemplate.opsForValue().increment(key, quantity);
    }
}