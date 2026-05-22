package com.mall.demo.module.coupon.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class GetMyCouponsRespDto {

    // 用户优惠券ID
    private String couponId;


    //coupon-user表字段
    private String couponName;
    // 优惠券名称

    //coupon-user表字段
    private LocalDateTime startTime;
    private LocalDateTime endTime;


    //coupon-user表字段(只有在已使用状态下有意义)
    private LocalDateTime usedTime;

}
