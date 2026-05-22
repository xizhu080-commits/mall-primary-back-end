package com.mall.demo.module.product.dto.resp;


import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 商品详情响应 DTO (需要实现Serializable以支持Redis序列化)
 */
@Data
public class ProductDetailRespDto implements Serializable {
    private static final long serialVersionUID = 1L;

    //spu 主表
    private String spuId;
    private String productName;
    private String specName;

    //返回该商品的所有规格
    private List<ProductSkuRespDto> skuList;

    // 用于解决缓存穿透的空对象标记
    private boolean emptyObject = false;



    //sku 表
    @Data
    public static class ProductSkuRespDto implements Serializable {
        private static final long serialVersionUID = 1L;

        private String skuId;
        private String specData;
        private String productName;
        private Integer price;
        private String productUrl;
        private Integer stock;
    }
}

