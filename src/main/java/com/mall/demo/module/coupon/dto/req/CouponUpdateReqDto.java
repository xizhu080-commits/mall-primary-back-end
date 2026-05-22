package com.mall.demo.module.coupon.dto.req;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
public class CouponUpdateReqDto {

    private String couponTemplateId;

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
    private Integer getLimit;


}
