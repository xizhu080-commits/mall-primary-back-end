package com.mall.demo.module.order.dto.resp;

import com.mall.demo.module.coupon.dto.resp.CouponRespDto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderPreviewRespDto {


    //使用的优惠卷列表
    List<String> usedCouponIds ;

    BigDecimal totalAmount;
    BigDecimal payableAmount;
    BigDecimal discountAmount;


    //可用优惠卷列表
    List<CouponRespDto> availableCoupons ;
    //不可用优惠卷列表
    List<CouponRespDto> unavailableCoupons;

    //商品列表
    private List<OrderItemRespDto> items;



}
