package com.mall.demo.module.product.service;


import com.mall.demo.common.result.PageResp;
import com.mall.demo.module.product.dto.req.ProductPublishReqDto;
import com.mall.demo.module.product.dto.req.ProductPageSearchReqDto;
import com.mall.demo.module.product.dto.resp.ProductDetailRespDto;
import com.mall.demo.module.product.dto.resp.ProductPageRespDto;
import com.mall.demo.module.product.dto.resp.ProductRecommendRespDto;

import java.util.List;

public interface ProductService {
    /**
     * 发布商品 (上架)
     */
    void publishProduct(ProductPublishReqDto dto);

    /**
     * 撤销商品 (下架)
     */
    void revokeProduct(String skuId, String spuId);

    /**
     * 搜索商品 (模糊查询分页)
     */
    PageResp<ProductPageRespDto> pageSearchProducts(ProductPageSearchReqDto dto);



    /*
    * 商品推荐:首页推荐
    * */
    List<ProductRecommendRespDto> productRecommend();


    /**
     * 获取商品详情 (★核心：抗高并发)
     */
    ProductDetailRespDto getProductDetail(String spuId);
}