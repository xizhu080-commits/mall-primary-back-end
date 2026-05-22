package com.mall.demo.module.product.dto.resp;

import lombok.Data;

/**
 * 商品分页列表响应 DTO
 */
@Data
public class ProductPageRespDto {

    private String spuId;
    private String productName;
    private String productUrl;
    private String specData;
    private Double price;

}