package com.mall.demo.module.order.service;


public interface StockService {

    /**
     * 扣减库存（Redis预减🔥）
     * @param skuId 商品ID
     * @param num 数量
     * @return 剩余库存（<=0 表示失败）
     */
    boolean deductStock(String skuId, Integer num);

    /**
     * 回滚库存（Redis）
     */
    void addStock(String skuId, Integer num);
}