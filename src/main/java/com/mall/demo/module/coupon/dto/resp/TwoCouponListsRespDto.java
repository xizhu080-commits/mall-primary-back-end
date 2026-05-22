package com.mall.demo.module.coupon.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class TwoCouponListsRespDto {

    List<CouponRespDto> availableCoupons;
    List<CouponRespDto> unavailableCoupons;
}
