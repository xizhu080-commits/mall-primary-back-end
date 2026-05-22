package com.mall.demo.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.product.dto.resp.ProductDetailRespDto;
import com.mall.demo.module.product.entity.SKU;

import com.mall.demo.module.shop.dto.resp.GetShopInfoRespDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


import java.util.List;


@Mapper
public interface SKUMapper extends BaseMapper<SKU> {


    /**
     * 根据商品ID查询所有SKU（规格）
     */
    @Select("""
    SELECT 
        sku_id AS skuId,
        spu_id AS spuId,
        spec_data AS specData,
        product_name AS productName,
        price,
        stock,
        productUrl
    FROM sku
    WHERE spu_id = #{spuId}
      AND status = 1
    ORDER BY price ASC
    """)
    List<ProductDetailRespDto.ProductSkuRespDto> selectBySpuId(@Param("spuId") String spuId);


    /*
    * 获取店铺的商品
    *
    * */
// ... existing code ...

    @Select(
            """
            SELECT 
                sku.sku_id AS skuId,
                sku.spu_id AS spuId,
                sku.spec_data AS specData,
                sku.product_name AS productName,
                sku.price,
                sku.stock,
                sku.productUrl,
                spu.category_id AS categoryId,
                pc.category_name AS categoryName
            FROM sku
            INNER JOIN spu ON sku.spu_id = spu.spu_id
            LEFT JOIN product_category pc ON spu.category_id = pc.category_id
            WHERE sku.shop_id = #{shopId}
              AND sku.status  IN (1, 2)
              AND spu.status  IN (1, 2)
            ORDER BY sku.create_time DESC
            """
    )
    List<GetShopInfoRespDto.GetSkuInfoRespDto> getShopProducts(@Param("shopId") String shopId);

// ... existing code ...


/*
* 查询特定商品的价格
* */
    @Select("""
    SELECT 
        price,
    FROM sku
    WHERE status = 1
        AND    sku_id = #{skuId}
    ORDER BY price ASC
    """)
    ProductDetailRespDto.ProductSkuRespDto selectPrice(@Param("skuId") String skuId);
}