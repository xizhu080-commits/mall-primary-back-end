package com.mall.demo.module.coupon.service;


import com.mall.demo.common.result.PageResp;

import com.mall.demo.module.coupon.dto.req.CouponListPublishReqDto;
import com.mall.demo.module.coupon.dto.req.CouponUpdateReqDto;
import com.mall.demo.module.coupon.dto.req.ReceiveCouponReqDto;
import com.mall.demo.module.coupon.dto.req.GetMyCouponsReqDto;
import com.mall.demo.module.coupon.dto.resp.*;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {


    /*
    * 计算应付金额
    * */
    public AmountPayableRespDto calculateAmountPayable(

            //优惠卷列表 couponId集合
            List<String> useCouponIds,
            String userId,
            BigDecimal totalAmount);


    /*
    * 返回两个优惠卷列表
    * */
    public TwoCouponListsRespDto getTwoCouponLists(String userId,   BigDecimal totalAmount);


    /*
    * 发布优惠卷(官方/店铺/商家)
    * */
    public void publishCoupons(CouponListPublishReqDto dto);

    /*
    * 修改优惠卷
    * */
    public void updateCoupons(CouponUpdateReqDto dto);

    /*
    * 领取优惠卷
    * */
    public String receiveCoupon(ReceiveCouponReqDto dto);


    /*
    * 查询我的优惠卷
    * */
    public PageResp<GetMyCouponsRespDto> getMyCoupons(GetMyCouponsReqDto dto);


    boolean useCouponsToRecord(String userId, List<String> usedCouponIds, String orderId);
}
