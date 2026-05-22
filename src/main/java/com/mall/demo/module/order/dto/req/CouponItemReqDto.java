package com.mall.demo.module.order.dto.req;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponItemReqDto {

    private String couponId;
    // 用户优惠券ID

    private String couponName;
    // 优惠券名称

    private Integer couponType;
    // 1满减 2折扣

    private BigDecimal thresholdAmount;
    // 满减门槛
    private BigDecimal discountAmount;
    // 减免金额
    private BigDecimal discountRate;
    // 折扣率


    private Integer stackable;
    // 是否可叠加   0 不可 1 可

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // ❗关键字段（只在不可用列表有意义）
    private String unusableReason;
}
