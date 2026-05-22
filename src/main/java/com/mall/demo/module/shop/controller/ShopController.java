package com.mall.demo.module.shop.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.shop.dto.req.CreateShopReqDto;
import com.mall.demo.module.shop.dto.resp.GetShopInfoRespDto;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Tag(name = "店铺管理", description = "店铺创建、管理接口")
public class ShopController {

    private final ShopService shopService;

    /**
     * 创建店铺
     */
    @Operation(summary = "创建店铺")
    @PostMapping("/create")
    public RestResp<String> createShop(@Valid @RequestBody CreateShopReqDto dto) {
        shopService.createShop(dto);
        return RestResp.ok("店铺创建成功");
    }

    /**
     * 获取店铺信息
     */
    @Operation(summary = "获取店铺信息")
    @GetMapping("/getShopInfo")
    public RestResp<GetShopInfoRespDto> getShopInfo(@RequestParam String shopId) {
        return RestResp.ok(shopService.getShopInfo(shopId));
    }


    /*
    * 获取商家的所有店铺
    * */
    @Operation(summary = "获取商家的所有店铺")
    @GetMapping("/getShopListByMerchantId")
    public RestResp<List<Shop>> getShopListByMerchantId() {
        return RestResp.ok(shopService.getShopListByMerchantId());
    }



}