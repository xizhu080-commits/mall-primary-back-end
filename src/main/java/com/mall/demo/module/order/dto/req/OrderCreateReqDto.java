package com.mall.demo.module.order.dto.req;

import com.mall.demo.module.coupon.dto.resp.CouponRespDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateReqDto {

    /*
    * 订单商品列表
    * */

    private List<OrderItemReqDto> items;




    // ⭐ 用户勾选的优惠券ID集合
    // 👉 第一次请求：null 或 []
    // 👉 用户勾选后：["c1","c2"]
    private List<String> usedCouponIds;


/*
* 拓展:地址,备注
* */
    @NotEmpty(message = "收货地址不能为空")
    private String address;


    //收货人
    @NotEmpty(message = "收货人不能为空")
    private String receiverName;
    //手机号
    @NotEmpty(message = "手机号不能为空")
    private String receiverPhone;


    private String remark;
}
