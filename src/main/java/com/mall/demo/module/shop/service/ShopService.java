package com.mall.demo.module.shop.service;

import com.mall.demo.module.shop.dto.req.CreateShopReqDto;
import com.mall.demo.module.shop.dto.resp.GetShopInfoRespDto;
import com.mall.demo.module.shop.entity.Shop;

import java.util.List;

public interface ShopService {


    void createShop(CreateShopReqDto dto);


    GetShopInfoRespDto getShopInfo(String shopId);

    /*
    * 获取商家的所有店铺
    * */
    List<Shop> getShopListByMerchantId();

}
