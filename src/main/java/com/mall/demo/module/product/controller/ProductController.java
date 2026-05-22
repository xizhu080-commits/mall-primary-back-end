package com.mall.demo.module.product.controller;


import com.mall.demo.common.result.PageResp;
import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.product.dto.req.ProductPublishReqDto;
import com.mall.demo.module.product.dto.req.ProductPageSearchReqDto;
import com.mall.demo.module.product.dto.resp.ProductDetailRespDto;
import com.mall.demo.module.product.dto.resp.ProductPageRespDto;
import com.mall.demo.module.product.dto.resp.ProductRecommendRespDto;
import com.mall.demo.module.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    @PostMapping("/publish")
    public RestResp<Void> publish(@RequestBody ProductPublishReqDto dto) {
        productService.publishProduct(dto);
        return RestResp.ok("商品发布成功", null);
    }
    // 商品推荐接口，返回推荐商品列表
    @GetMapping("/recommend")
    public RestResp<List<ProductRecommendRespDto>> recommend() {
        return RestResp.ok(productService.productRecommend());
    }

    @PostMapping("/revoke/{skuId}/{spuId}")
    public RestResp<Void> revoke(@PathVariable String skuId, @PathVariable String spuId) {
        productService.revokeProduct(skuId, spuId);
        return RestResp.ok();
    }



    // 商品搜索接口，分页模糊查询
    @GetMapping("/search")
    public RestResp<PageResp<ProductPageRespDto>> search(ProductPageSearchReqDto dto) {
        return RestResp.ok(productService.pageSearchProducts(dto));
    }





    // 商品详情接口，返回商品详情信息
    @GetMapping("/detail/{spuId}")
    public RestResp<ProductDetailRespDto> detail(@PathVariable String spuId) {
        return RestResp.ok(productService.getProductDetail(spuId));
    }
}
