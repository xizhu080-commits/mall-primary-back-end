package com.mall.demo.module.coupon.dto.req;


import lombok.Data;



@Data
public class GetMyCouponsReqDto {

    //'0未使用 1已使用 2已过期 '
    private Integer status;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
