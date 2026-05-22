package com.mall.demo.module.coupon.dto.req;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CouponListPublishReqDto {
    //商家或店铺或官方  发布的优惠卷列表


    private String couponName;
    private Integer couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer stackable;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalCount;
    private Integer status;
    private Integer activeType;
    private Integer publisherType;
    //设置每个用户可以领取几张
    private Integer getLimit;


}
