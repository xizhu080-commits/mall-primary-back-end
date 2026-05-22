package com.mall.demo.module.shop.dto.resp;

import com.mall.demo.module.coupon.entity.CouponTemplate;
import com.mall.demo.module.shop.entity.Shop;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetShopInfoRespDto extends Shop {

    List<CouponTemplate> shopCouponList;

    List<GetSkuInfoRespDto> shopProductList;


    @Data
    public static class GetSkuInfoRespDto implements Serializable {
        private String skuId;
        private String spuId;
        private String specData;
        private String productName;
        private BigDecimal price;
        private String productUrl;
        private Integer stock;
        private Integer categoryId;
        private String categoryName;
    }

}
