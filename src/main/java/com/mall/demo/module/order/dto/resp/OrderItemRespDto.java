package com.mall.demo.module.order.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemRespDto {


    private String skuId;
    private Integer quantity;
    private BigDecimal price;
    private String productName;
    private String productUrl;

}
