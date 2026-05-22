package com.mall.demo.module.product.dto.req;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


/**
 * 商品发布/上架请求 DTO
 */
@Data
public class ProductPublishReqDto {


    @NotBlank(message = "商品名称不能为空")
    private String productName;
    @NotNull(message = "分类ID不能为空")
    private String categoryId;
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    @NotNull(message = "商品价格不能为空")
    private BigDecimal price;
    @NotBlank(message = "商品图片不能为空")
    private String productUrl;
    private String specName;
    // SPU下的多个规格SKU
    @NotNull(message = "SKU列表不能为空")
    private List<ProductSkuReqDto> skuList;


    @Data
    public static class ProductSkuReqDto {



        @NotBlank(message = "商品名称不能为空")
        private String productName;

        @NotBlank(message = "商品图片不能为空")
        private String productUrl;

        private Integer status;
        // 商品状态：1-上架 2-下架 3- 预售

        @NotBlank(message = "规格数据不能为空")
        private String specData; // 规格属性，如JSON格式: {"color":"red", "size":"XL"}

        @NotNull(message = "商品价格不能为空")
        private BigDecimal price;

        @NotNull(message = "库存数量不能为空")
        private Integer stock;



        private LocalDateTime expireTime;


    }
}

