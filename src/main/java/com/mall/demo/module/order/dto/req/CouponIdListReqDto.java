package com.mall.demo.module.order.dto.req;

import com.mall.demo.module.coupon.dto.resp.CouponRespDto;
import lombok.Data;

import java.util.List;

@Data
public class CouponIdListReqDto {

    private List<CouponRespDto> couponItem;

}
