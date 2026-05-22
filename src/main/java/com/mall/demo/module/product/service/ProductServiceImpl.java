package com.mall.demo.module.product.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.common.result.PageResp;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.merchant.entity.Merchant;
import com.mall.demo.module.merchant.mapper.MerchantMapper;
import com.mall.demo.module.product.dto.req.ProductPublishReqDto;
import com.mall.demo.module.product.dto.req.ProductPageSearchReqDto;
import com.mall.demo.module.product.dto.resp.ProductDetailRespDto;
import com.mall.demo.module.product.dto.resp.ProductPageRespDto;
import com.mall.demo.module.product.dto.resp.ProductRecommendRespDto;
import com.mall.demo.module.product.entity.Product_Category;
import com.mall.demo.module.product.entity.SKU;
import com.mall.demo.module.product.entity.SPU;
import com.mall.demo.module.product.mapper.Product_CategoryMapper;
import com.mall.demo.module.product.mapper.SKUMapper;
import com.mall.demo.module.product.mapper.SPUMapper;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import com.mall.demo.module.user.entity.User;
import com.mall.demo.module.user.mapper.UserMapper;
import com.mall.demo.mq.producer.ProductProducer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    // 假设这些Mapper已经存在
    private final SPUMapper spuMapper;
    private final SKUMapper skuMapper;
    private final Product_CategoryMapper categoryMapper;
    private final ProductProducer productProducer;
    private final RedisService redisService;
    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;
    private final Product_CategoryMapper product_CategoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishProduct(ProductPublishReqDto dto) {


        // 1. 从请求头获取 shopId（不是从 Token）
        // 获取店铺ID（从请求头提取后存入 SecurityUtils）
        String shopId = SecurityUtils.getCurrentShopId();
        log.info("shopId: {}", shopId);
        // 从请求头获取

        // 2. 获取当前登录的商家ID（从 Token 获取）
        String merchantId = SecurityUtils.getId();



        Merchant merchant = merchantMapper.selectById(merchantId);
        LambdaQueryWrapper<Shop> shopLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shopLambdaQueryWrapper.eq(Shop::getMerchantId, merchantId)
                .eq(Shop::getShopId, shopId);


        if (merchant == null) {
            throw new BizException(ErrorCodeEnum.NO_PERMISSION.getCode(), ErrorCodeEnum.NO_PERMISSION.getMessage());
        }
        // 4. 校验商家是否有权限操作该店铺


        Shop shop = shopMapper.selectOne(shopLambdaQueryWrapper);
        if (shop == null) {
            throw new BizException(ErrorCodeEnum.NO_PERMISSION.getCode(), "无权限操作该店铺");
        }

        //spu主表
        SPU spu = new SPU();
        spu.setProductName(dto.getProductName());
        spu.setCategoryId(dto.getCategoryId());
        spu.setProductUrl(dto.getProductUrl());
        spu.setShopId(shopId);
        spu.setPrice(dto.getPrice());

        spuMapper.insert(spu);

        //分类表
        Product_Category category = new Product_Category();
        category.setCategoryId(dto.getCategoryId());
        category.setCategoryName(dto.getCategoryName());
        category.setSpecName(dto.getSpecName());
        categoryMapper.insert(category);


        //sku 表
        for (ProductPublishReqDto.ProductSkuReqDto skuReqDto : dto.getSkuList()) {
            SKU sku = new SKU();
            sku.setSpuId(spu.getSpuId());
            sku.setSpecData(skuReqDto.getSpecData());
            sku.setPrice(skuReqDto.getPrice());
            sku.setStock(skuReqDto.getStock());
            sku.setStatus(skuReqDto.getStatus());
            sku.setShopId(shopId);
            sku.setProductUrl(skuReqDto.getProductUrl());
            sku.setProductName(spu.getProductName());
            sku.setCreateTime(LocalDateTime.now());
            sku.setUpdateTime(LocalDateTime.now());
            sku.setExpireTime(skuReqDto.getExpireTime());
            skuMapper.insert(sku);

            if (skuReqDto.getExpireTime() != null) {
                Long delay = Duration.between(LocalDateTime.now(), skuReqDto.getExpireTime()).toMillis();
                if (delay == 0) {
                    productProducer.sendRevokeSku(sku.getSkuId());
                    productProducer.sendDeleteSku(sku.getSpuId());
                }
            }

            // 4. 触发消息队列，将商品信息发送到缓存中心
            productProducer.sendPublishSpu(spu);
            productProducer.sendPublishSku(sku);



            log.info("商品{}发布成功", sku.getProductName());
        }
    }









    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeProduct(String skuId, String spuId) {

        // 从请求头获取 shopId（不是从参数传）
        String shopId = SecurityUtils.getCurrentShopId();

        // 获取当前登录的商家ID
        String merchantId = SecurityUtils.getId();

        // 校验商家是否有权限操作该店铺
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getMerchantId, merchantId)
                .eq(Shop::getShopId, shopId);
        Shop shop = shopMapper.selectOne(shopWrapper);
        if (shop == null) {
            throw new BizException(ErrorCodeEnum.NO_PERMISSION.getCode(), "无权限操作该店铺");
        }

        // 校验 SKU 是否属于该店铺
        SKU sku = skuMapper.selectById(skuId);
        if (sku == null || !sku.getShopId().equals(shopId)) {
            throw new BizException(ErrorCodeEnum.NO_PERMISSION.getCode(), "无权限操作该商品");
        }

        // 更新 SKU 状态
        sku.setStatus(0);
        sku.setUpdateTime(LocalDateTime.now());
        skuMapper.updateById(sku);

        // 发送消息
        productProducer.sendRevokeSku(skuId);
    }






    // 分页查询商品:模糊查询
    @Override
    public PageResp<ProductPageRespDto> pageSearchProducts(ProductPageSearchReqDto dto) {

        String keyword = dto.getKeyword().trim();
        int pageNo = dto.getPageNo();
        int pageSize = dto.getPageSize();


        if (keyword.length() < 2){
            // 1️⃣ 计算offset
            int offset = (pageNo - 1) * pageSize;

            // 2️⃣ 查询列表
            List<ProductPageRespDto> list =
                    spuMapper.pageSearchProducts(keyword, offset, pageSize);

            // 3️⃣ 查询总数（必须有🔥）
            Long total = spuMapper.countSearchProducts(keyword);

            // 4️⃣ 返回分页
            return PageResp.of(
                    total,
                    (long) pageSize,
                    (long) pageNo,
                    list
            );
        }


        //查询缓存
        String key = "search:" + keyword + ":" + pageNo + ":" + pageSize;
        return redisService.getPage(key, ProductPageRespDto.class,
                (k) -> {
                    // 1️⃣ 计算offset
                    //作用：每页从第几条数据开始     如第一页：0~10条数据  第二页：10~20条数据
                    int offset = (pageNo - 1) * pageSize;

                    // 2️⃣ 查询列表
                    List<ProductPageRespDto> list =
                            spuMapper.pageSearchProducts(keyword, offset, pageSize);

                    // 3️⃣ 查询总数（必须有🔥）
                    Long total = spuMapper.countSearchProducts(keyword);

                    // 4️⃣ 返回分页
                    return PageResp.of(
                            total,
                            (long) pageSize,
                            (long) pageNo,
                            list
                    );
                },
                10
        );

    }




    @Override
    public List<ProductRecommendRespDto> productRecommend() {

        //查询缓存
        String key = "home:recommend";
        return redisService.getList(
                key,
                ProductRecommendRespDto.class,
                k -> {
                    return spuMapper.selectRecommend();
                },
                10
        );
    }





// ... existing code ...
    @Override
    public ProductDetailRespDto getProductDetail(String spuId) {

        log.info("开始查询商品详情，spuId: {}", spuId);

        String key = "product:detail:" + spuId;



        ProductDetailRespDto productDetailRespDto = redisService.get(
                key,
                ProductDetailRespDto.class,
                k -> {

                    log.info("缓存未命中，从数据库查询商品详情，spuId: {}", spuId);

                    SPU spu = spuMapper.selectById(spuId);
                    if (spu == null){
                        log.error("商品不存在，spuId: {}", spuId);
                        throw new BizException(ErrorCodeEnum.PRO_NOT_FOUND.getCode(), ErrorCodeEnum.PRO_NOT_FOUND.getMessage());
                    }

                    log.info("查询到 SPU 信息，spuId: {}, productName: {}, categoryId: {}",
                            spu.getSpuId(), spu.getProductName(), spu.getCategoryId());


                    List<ProductDetailRespDto.ProductSkuRespDto> skuList = skuMapper.selectBySpuId(spuId);
                    if (skuList == null || skuList.isEmpty()){
                        log.error("商品 SKU 列表为空，spuId: {}", spuId);
                        throw new BizException(ErrorCodeEnum.PRO_NOT_FOUND.getCode(), ErrorCodeEnum.PRO_NOT_FOUND.getMessage());
                    }

                    log.info("查询到 {} 个 SKU", skuList.size());



                    LambdaQueryWrapper<Product_Category> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(Product_Category::getCategoryId, spu.getCategoryId());
                    Product_Category category = product_CategoryMapper.selectOne(wrapper);

                    String specName = null;
                    if (category != null) {
                        specName = category.getSpecName();
                        log.info("查询到分类信息，categoryId: {}, categoryName: {}, specName: {},",
                                spu.getCategoryId(), category.getCategoryName(), specName);
                    } else {
                        log.warn("未找到分类ID为 {} 的分类记录，请检查 product_category 表是否有对应数据", spu.getCategoryId());
                    }





                    ProductDetailRespDto resp = new ProductDetailRespDto();
                    resp.setSpuId(spuId);
                    resp.setProductName(spu.getProductName());
                    resp.setSpecName(specName);
                    resp.setSkuList(skuList);

                    log.info("商品详情组装完成，spuId: {}, 分类ID:{}, 规格名称: {}", spuId, spu.getCategoryId(), specName);
                    return resp;
                },
                60
        );



        log.info("商品详情查询完成，spuId: {}, specName: {}",
                spuId,
                productDetailRespDto != null ? productDetailRespDto.getSpecName() : "null");

        return productDetailRespDto;
    }

    /**
     * 清理商品详情缓存（用于调试或数据更新后）
     */
    public void clearProductDetailCache(String spuId) {
        String key = "product:detail:" + spuId;
        redisService.delete(key);
        log.info("已清理商品详情缓存，spuId: {}", spuId);
    }
// ... existing code ...












}
