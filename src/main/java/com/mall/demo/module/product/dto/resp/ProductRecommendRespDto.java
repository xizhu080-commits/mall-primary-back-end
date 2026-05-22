package com.mall.demo.module.product.dto.resp;

import lombok.Data;

@Data
public class ProductRecommendRespDto {

    private String spuId;
    private String skuId;
    private Integer categoryId;
    private String productName;
    private String productUrl;
    private String specData;
    private Double price;

}
