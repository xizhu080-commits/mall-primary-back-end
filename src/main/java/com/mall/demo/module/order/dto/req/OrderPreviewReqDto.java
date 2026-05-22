package com.mall.demo.module.order.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class OrderPreviewReqDto {

    private Boolean isFirstRequest;

    // 商品列表
    private List<OrderItemReqDto> items;

    // ⭐ 用户勾选的优惠券ID集合
    // 👉 第一次请求：null 或 []
    // 👉 用户勾选后：["c1","c2"]
    //传入couponId列表
    private List<String> usedCouponIds;
}