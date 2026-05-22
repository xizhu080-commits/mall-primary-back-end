package com.mall.demo.module.shop.service;


import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.coupon.entity.CouponPublisher;
import com.mall.demo.module.coupon.entity.CouponTemplate;
import com.mall.demo.module.coupon.mapper.CouponPublisherMapper;
import com.mall.demo.module.coupon.mapper.CouponTemplateMapper;
import com.mall.demo.module.product.mapper.SKUMapper;
import com.mall.demo.module.shop.dto.req.CreateShopReqDto;
import com.mall.demo.module.shop.dto.resp.GetShopInfoRespDto;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final CouponPublisherMapper couponPublisherMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final SKUMapper skuMapper;
    private final RedisService redisService;


    /*
     * 创建店铺
     * */
    @Override
    public void createShop(CreateShopReqDto dto) {

        // 1. 获取当前登录商家ID
        String merchantId = SecurityUtils.getId();
        if (merchantId == null) {
            throw new BizException(ErrorCodeEnum.MERCHANT_NOT_EXIST.getCode(), "商家不存在");
        }

        // 2. 参数校验
        String shopName = dto.getShopName();
        if (StrUtil.isBlank(shopName)) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR.getCode(), "店铺名称不能为空");
        }

        String shopLogo = dto.getShopLogo();

        // ========== 新增：幂等性校验（防止重复提交）==========
        String idempotentKey = "idempotent:create_shop:" + merchantId;
        Boolean setSuccess = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", Duration.ofSeconds(5));

        if (Boolean.FALSE.equals(setSuccess)) {
            throw new BizException(ErrorCodeEnum.MANY_REQUEST.getCode(), ErrorCodeEnum.MANY_REQUEST.getMessage());
        }

        try {
            // ========== 新增：分布式锁（防止并发冲突）==========
            String lockKey = "lock:create_shop:" + merchantId;
            RLock lock = redissonClient.getLock(lockKey);

            try {
                // 尝试加锁，等待3秒，10秒后自动释放
                boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!locked) {
                    throw new BizException(ErrorCodeEnum.SYSTEM_BUSY.getCode(), ErrorCodeEnum.SYSTEM_BUSY.getMessage());
                }

                // ========== 双重检查：加锁后再次查询（防止并发）==========
                LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Shop::getMerchantId, merchantId)
                        .eq(Shop::getShopName, shopName);
                Long count = shopMapper.selectCount(wrapper);

                if (count > 0) {
                    throw new BizException(ErrorCodeEnum.SHOP_EXIST.getCode(),
                            "您已存在名为「" + shopName + "」的店铺，请更换店铺名称");
                }

                // 创建店铺
                Shop shop = new Shop();

                shop.setMerchantId(merchantId);
                shop.setShopName(shopName);
                shop.setShopLogo(shopLogo);
                shop.setWorkStatus(1);
                shop.setRating(0.00);
                shop.setCreateTime(LocalDateTime.now());
                shop.setUpdateTime(LocalDateTime.now());

                shopMapper.insert(shop);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BizException(ErrorCodeEnum.SYSTEM_BUSY.getCode(), ErrorCodeEnum.SYSTEM_BUSY.getMessage());
            } finally {
                // 释放分布式锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } finally {
            // 释放幂等标记（可选：业务成功后保留一点时间）
            // 注意：如果希望业务成功后在5秒内也不能重复，可以删除这行
            stringRedisTemplate.delete(idempotentKey);
        }
    }








    @Override
    public GetShopInfoRespDto getShopInfo(String shopId) {
        log.info("开始查询店铺信息，shopId: {}", shopId);

        if (StrUtil.isBlank(shopId)) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR.getCode(), "店铺ID不能为空");
        }

        String cacheKey = "shop:detail:" + shopId;

        GetShopInfoRespDto respDto = redisService.get(
                cacheKey,
                GetShopInfoRespDto.class,
                key -> {
                    Shop shop = shopMapper.selectById(shopId);
                    if (shop == null) {
                        log.warn("店铺不存在，shopId: {}", shopId);
                        return null;
                    }

                    List<String> templateIds = couponPublisherMapper.getTemplateIdsByShopId(shopId);

                    List<CouponTemplate> shopCouponList = new ArrayList<>();
                    if (templateIds != null && !templateIds.isEmpty()) {
                        for (String templateId : templateIds) {
                            CouponTemplate couponTemplate = couponTemplateMapper.getAvailableTemplateByTemplateId(templateId);
                            if (couponTemplate != null) {
                                shopCouponList.add(couponTemplate);
                            }
                        }
                    }

                    log.info("开始查询店铺商品，shopId: {}", shopId);
                    List<GetShopInfoRespDto.GetSkuInfoRespDto> shopProductList = skuMapper.getShopProducts(shopId);

                    if (shopProductList == null) {
                        shopProductList = new ArrayList<>();
                        log.warn("店铺商品列表为null，已初始化为空列表，shopId: {}", shopId);
                    } else {
                        log.info("查询到店铺商品数量: {}, shopId: {}", shopProductList.size(), shopId);
                    }

                    GetShopInfoRespDto dto = new GetShopInfoRespDto();
                    dto.setShopId(shop.getShopId());
                    dto.setMerchantId(shop.getMerchantId());
                    dto.setShopName(shop.getShopName());
                    dto.setShopLogo(shop.getShopLogo());
                    dto.setWorkStatus(shop.getWorkStatus());
                    dto.setRating(shop.getRating());
                    dto.setCreateTime(shop.getCreateTime());
                    dto.setUpdateTime(shop.getUpdateTime());
                    dto.setShopCouponList(shopCouponList);
                    dto.setShopProductList(shopProductList);

                    log.info("从数据库查询店铺信息成功，shopId: {}, 优惠券数量: {}, 商品数量: {}",
                            shopId, shopCouponList.size(), shopProductList.size());
                    return dto;
                },
                30
        );

        if (respDto == null) {
            throw new BizException(ErrorCodeEnum.SHOP_NOT_EXIST.getCode(), ErrorCodeEnum.SHOP_NOT_EXIST.getMessage());
        }

        log.info("查询店铺信息完成，shopId: {}", shopId);
        return respDto;
    }








    @Override
    public List<Shop> getShopListByMerchantId() {

        String merchantId = SecurityUtils.getId();

        if (merchantId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR.getCode(), "商家ID不能为空");
        }

        List<Shop> shopList = shopMapper.getShopListByMerchantId(merchantId);


        if (shopList == null || shopList.isEmpty()) {
            log.warn("商家没有店铺，merchantId: {}", merchantId);
            return new ArrayList<>();
        }
        return shopList;





    }





}