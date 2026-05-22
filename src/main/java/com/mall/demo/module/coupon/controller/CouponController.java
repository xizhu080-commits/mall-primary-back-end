package com.mall.demo.module.coupon.controller;


import com.mall.demo.common.result.PageResp;
import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.coupon.dto.req.CouponListPublishReqDto;
import com.mall.demo.module.coupon.dto.req.ReceiveCouponReqDto;
import com.mall.demo.module.coupon.dto.req.GetMyCouponsReqDto;
import com.mall.demo.module.coupon.dto.resp.GetMyCouponsRespDto;
import com.mall.demo.module.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/coupon")
public class CouponController {



    private final CouponService couponService;


    /*
    * 发布优惠卷
    * */
    @PostMapping("/publishCoupons")
    public String publishCoupon(@Validated @RequestBody CouponListPublishReqDto dto) {
        couponService.publishCoupons(dto);
        return "发布优惠卷成功";
    }





    // 领取优惠券
    @Operation(summary = "领取优惠券")
    @PostMapping("/receiveCoupon")
    public RestResp<String> receiveCoupon(@Validated @RequestBody ReceiveCouponReqDto dto) {
        log.info("优惠卷模板ID:{}",dto.getCouponTemplateId());
        return RestResp.ok(couponService.receiveCoupon(dto));
    }






    // 查询我的优惠券
    @GetMapping("/getMyCoupons")
    public RestResp<PageResp<GetMyCouponsRespDto>> getMyCoupons(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {

        GetMyCouponsReqDto dto = new GetMyCouponsReqDto();
        dto.setStatus(status);
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);

        PageResp<GetMyCouponsRespDto> result = couponService.getMyCoupons(dto);
        return RestResp.ok(result);
    }
}
