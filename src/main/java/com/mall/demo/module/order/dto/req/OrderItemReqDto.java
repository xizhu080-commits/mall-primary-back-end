package com.mall.demo.module.order.dto.req;

import lombok.Data;

@Data
public class OrderItemReqDto {

    private String skuId;
    private Integer quantity;
}