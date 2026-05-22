package com.mall.demo.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.product.dto.resp.ProductPageRespDto;
import com.mall.demo.module.product.dto.resp.ProductRecommendRespDto;
import org.apache.ibatis.annotations.Mapper;
import com.mall.demo.module.product.entity.SPU;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SPUMapper extends BaseMapper<SPU> {

    //分页查询商品
    @Select("""
SELECT  spu_id AS spuId, product_name AS productName, productUrl AS productUrl, price
FROM spu
WHERE product_name LIKE CONCAT('%', #{keyword}, '%')
LIMIT #{offset}, #{size}
""")
    List<ProductPageRespDto> pageSearchProducts(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    //查询商品总数
    @Select("""
SELECT COUNT(DISTINCT spu_id)
FROM spu
WHERE product_name LIKE CONCAT('%', #{keyword}, '%')
""")
    Long countSearchProducts(@Param("keyword") String keyword);

    //首页推荐
    @Select("""
SELECT 
    spu_id AS spuId,
    product_name AS productName,
    productUrl AS productUrl,
    price AS price
FROM spu
WHERE status = 1
LIMIT 10
""")
    List<ProductRecommendRespDto> selectRecommend();

}




