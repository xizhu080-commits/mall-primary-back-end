package com.mall.demo.module.coupon.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.result.PageResp;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.coupon.dto.req.*;
import com.mall.demo.module.coupon.dto.resp.AmountPayableRespDto;
import com.mall.demo.module.coupon.dto.resp.CouponRespDto;
import com.mall.demo.module.coupon.dto.resp.GetMyCouponsRespDto;
import com.mall.demo.module.coupon.dto.resp.TwoCouponListsRespDto;
import com.mall.demo.module.coupon.entity.*;
import com.mall.demo.module.coupon.entity.CouponReceiveRecord;
import com.mall.demo.module.coupon.mapper.*;
import com.mall.demo.module.merchant.mapper.MerchantMapper;
import com.mall.demo.module.order.mapper.OrderMapper;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import com.mall.demo.module.user.entity.User;
import com.mall.demo.module.user.mapper.UserMapper;
import com.mall.demo.mq.producer.CouponProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final OrderMapper orderMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponUserMapper couponUserMapper;
    private final UserMapper userMapper;
    private final CouponProducer couponProducer;
    private final MerchantMapper merchantMapper;
    private final CouponReceiveRecordMapper couponReceiveRecordMapper;
    private final CouponPublisherMapper couponPublisherMapper;
    private final ShopMapper shopMapper;
    private final CouponUsedRecordMapper couponUsedRecordMapper;


    /*
     * 返回两个优惠卷列表
     *
     * */
    @Override
    public TwoCouponListsRespDto getTwoCouponLists(String userId, BigDecimal totalAmount) {

        //查询数据库,获取我的所有优惠卷
        List<CouponUser> allCoupon = couponUserMapper.getAllCoupon(userId);
        if (allCoupon == null || allCoupon.isEmpty()) {
            return null;
        }
        //定义两个列表
        List<CouponRespDto> availableCoupons = new ArrayList<>();
        List<CouponRespDto> unavailableCoupons = new ArrayList<>();


        //循环优惠券列表
        for (CouponUser couponUser : allCoupon) {
            //获取我的优惠卷的优惠券模板
            CouponTemplate couponTemplate = couponTemplateMapper.selectById(couponUser.getTemplateId());

            if (couponTemplate == null) {
                continue;
            }

            //门槛金额,获取我的优惠卷的门槛
            BigDecimal thresholdAmount = couponTemplate.getThresholdAmount();
            if (thresholdAmount == null) {
                thresholdAmount = BigDecimal.ZERO;
            }


            //获取我的不可用优惠卷
            //门槛金额与订单总金额比较   > 0 为TRUE              status : '0未使用 1已使用 2已过期
            if (thresholdAmount.compareTo(totalAmount) > 0 || couponUser.getStatus() != 0 || couponUser.getStartTime().isAfter(LocalDateTime.now()) || couponUser.getEndTime().isBefore(LocalDateTime.now()))  {
                //门槛大 - 不可用
                unavailableCoupons.add(CouponRespDto.builder()
                        .couponId(couponUser.getCouponId())
                        .couponName(couponTemplate.getCouponName())
                        .couponType(couponTemplate.getCouponType())
                        .thresholdAmount(thresholdAmount)
                        .discountAmount(couponTemplate.getDiscountAmount())
                        .discountRate(couponTemplate.getDiscountRate())
                        .stackable(couponTemplate.getStackable())
                        .startTime(couponTemplate.getStartTime())
                        .endTime(couponTemplate.getEndTime())
                        .unusableReason("订单金额低于优惠券门槛 || 优惠券状态异常")
                        .build());
            } else {
                // 可用
                availableCoupons.add(CouponRespDto.builder()
                        .couponId(couponUser.getCouponId())
                        .couponName(couponTemplate.getCouponName())
                        .couponType(couponTemplate.getCouponType())
                        .thresholdAmount(thresholdAmount)
                        .discountAmount(couponTemplate.getDiscountAmount())
                        .discountRate(couponTemplate.getDiscountRate())
                        .stackable(couponTemplate.getStackable())
                        .startTime(couponTemplate.getStartTime())
                        .endTime(couponTemplate.getEndTime())
                        .unusableReason(null)
                        .build());
            }
        }

        return TwoCouponListsRespDto.builder()
                .availableCoupons(availableCoupons)
                .unavailableCoupons(unavailableCoupons)
                .build();
    }


    public List<CouponRespDto> getUsedCoupons(String userId, List<String> useCouponIds, BigDecimal totalAmount) {
        //useCouponIds 为用户勾选的优惠券ID集合
        List<CouponRespDto> availableCoupons = getTwoCouponLists(userId, totalAmount).getAvailableCoupons();
        List<CouponRespDto> usedCoupons = new ArrayList<>();
        for (CouponRespDto couponRespDto : availableCoupons) {
            if (useCouponIds != null && useCouponIds.contains(couponRespDto.getCouponId())) {
                usedCoupons.add(CouponRespDto.builder()
                        .couponId(couponRespDto.getCouponId())
                        .couponName(couponRespDto.getCouponName())
                        .couponType(couponRespDto.getCouponType())
                        .thresholdAmount(couponRespDto.getThresholdAmount())
                        .discountAmount(couponRespDto.getDiscountAmount())
                        .discountRate(couponRespDto.getDiscountRate())
                        .stackable(couponRespDto.getStackable())
                        .startTime(couponRespDto.getStartTime())
                        .endTime(couponRespDto.getEndTime())
                        .build());
            }
        }

        return usedCoupons;


    }

    @Override
    public AmountPayableRespDto calculateAmountPayable(
            //优惠卷列表
            List<String> useCouponIds,
            String userId,
            BigDecimal totalAmount) {

        List<CouponRespDto> availableCoupons;

        // ✅ 修复点1：支持用户选券

        if (useCouponIds != null && !useCouponIds.isEmpty()) {
            availableCoupons = getUsedCoupons(userId, useCouponIds, totalAmount);
        } else {
            TwoCouponListsRespDto twoCouponLists = getTwoCouponLists(userId, totalAmount);
            if (twoCouponLists == null || twoCouponLists.getAvailableCoupons() == null) {
                availableCoupons = new ArrayList<>();
            } else {
                availableCoupons = twoCouponLists.getAvailableCoupons();
            }
        }


        // ✅ 修复点2：排序（关键）
        availableCoupons.sort((a, b) -> {
            BigDecimal amountA = a.getDiscountAmount() != null ? a.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal amountB = b.getDiscountAmount() != null ? b.getDiscountAmount() : BigDecimal.ZERO;
            return amountB.compareTo(amountA);
        });

        // 满减优惠券
        BigDecimal bestMinusDiscount = BigDecimal.ZERO;
        String bestMinusCouponId = null;

        //折扣优惠卷
        BigDecimal bestDiscountRate = new BigDecimal(1);
        String bestDiscountCouponId = null;

        //叠加优惠券
        List<String> stackableCouponIds = new ArrayList<>();
        BigDecimal stackableTotalDiscount = BigDecimal.ZERO;

  for (CouponRespDto coupon : availableCoupons) {

            //'是否可叠加：0否 1是'
            if (coupon.getStackable() == 0) {
                //不可叠加

                //'类型：1满减 2折扣'
                if (coupon.getCouponType() == 1) {
                    //满减
                    if (coupon.getDiscountAmount().compareTo(bestMinusDiscount) > 0) {
                        bestMinusDiscount = coupon.getDiscountAmount();
                        bestMinusCouponId = coupon.getCouponId();
                    }
                } else {
                    //折扣
                    if (coupon.getDiscountRate().compareTo(bestDiscountRate) < 0) {
                        bestDiscountRate = coupon.getDiscountRate();
                        bestDiscountCouponId = coupon.getCouponId();
                    }
                }

            }
            //'是否可叠加：0否 1是'
            else if (coupon.getCouponType() == 1) {
                //只有满减可以叠加啊
                BigDecimal potentialTotalDiscount =
                        stackableTotalDiscount.add(coupon.getDiscountAmount());

                // ✅ 修复点3：允许等于 totalAmount
                if (potentialTotalDiscount.compareTo(totalAmount) <= 0) {
                    stackableTotalDiscount = potentialTotalDiscount;
                    stackableCouponIds.add(coupon.getCouponId());
                }
            }
        }





        //满减
        BigDecimal payWithBestMinus = totalAmount;
        if (bestMinusDiscount.compareTo(BigDecimal.ZERO) > 0) {
            payWithBestMinus = totalAmount.subtract(bestMinusDiscount);
        }
        //折扣
        BigDecimal payWithBestDiscount = totalAmount;
        if (bestDiscountRate.compareTo(BigDecimal.ONE) < 0) {
            payWithBestDiscount = totalAmount.multiply(bestDiscountRate);
        }

        //叠加
        BigDecimal payWithStackable = totalAmount.subtract(stackableTotalDiscount);

        BigDecimal finalPayAmount;
        List<String> recommendCouponIds = new ArrayList<>();

  //获取最小的支付金额
        if (payWithBestMinus.compareTo(payWithBestDiscount) < 0) {
            if (payWithBestMinus.compareTo(payWithStackable) < 0) {
                //满减最优
                finalPayAmount = payWithBestMinus;
                if (bestMinusCouponId != null) {
                    recommendCouponIds.add(bestMinusCouponId);
                }
            } else {
                //叠加最优
                finalPayAmount = payWithStackable;
                recommendCouponIds.addAll(stackableCouponIds);
            }

        } else {
            if (payWithBestDiscount.compareTo(payWithStackable) < 0) {
                //折扣最优
                finalPayAmount = payWithBestDiscount;
                if (bestDiscountCouponId != null) {
                    recommendCouponIds.add(bestDiscountCouponId);
                }
            } else {
                //叠加最优
                finalPayAmount = payWithStackable;
                recommendCouponIds.addAll(stackableCouponIds);
            }
        }

        //打印couponId
        log.info("推荐的优惠卷为:{}", recommendCouponIds);

        AmountPayableRespDto resp = new AmountPayableRespDto();
        resp.setTotalAmount(totalAmount);
        resp.setPayableAmount(finalPayAmount);
        resp.setDiscountAmount(totalAmount.subtract(finalPayAmount));
        resp.setCouponIds(recommendCouponIds);

        return resp;
    }




    @Override
    public void publishCoupons(CouponListPublishReqDto dto) {

        //获取用户ID,再根据id查询role,判断是否是官方/店铺/商家
        String merchantId = SecurityUtils.getId();
        String shopId = SecurityUtils.getCurrentShopId();



        // 校验 shopId 是否存在
        if (shopId == null || shopId.isEmpty()) {
            throw new BizException(ErrorCodeEnum.HEADER_MISSING.getCode(),ErrorCodeEnum.HEADER_MISSING.getMessage());
        }

        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BizException(ErrorCodeEnum.SHOP_NOT_EXIST.getCode(), "店铺不存在");
        }

        // 校验商家是否有权限操作该店铺（商家ID必须匹配）
        if (!shop.getMerchantId().equals(merchantId)) {
            throw new BizException(ErrorCodeEnum.NO_PERMISSION.getCode(), "无权限操作该店铺");
        }



        CouponTemplate couponTemplate = new CouponTemplate();

        couponTemplate.setCouponName(dto.getCouponName());
        couponTemplate.setCouponType(dto.getCouponType());
        couponTemplate.setThresholdAmount(dto.getThresholdAmount());
        couponTemplate.setDiscountAmount(dto.getDiscountAmount());
        couponTemplate.setDiscountRate(dto.getDiscountRate());
        couponTemplate.setStackable(dto.getStackable());
        couponTemplate.setRemainCount(dto.getTotalCount());


        couponTemplate.setStartTime(dto.getStartTime());
        couponTemplate.setEndTime(dto.getEndTime());


        couponTemplate.setTotalCount(dto.getTotalCount());
        couponTemplate.setStatus(dto.getStatus());
        couponTemplate.setCreateTime(LocalDateTime.now());
        couponTemplate.setUpdateTime(LocalDateTime.now());
        couponTemplate.setActiveType(dto.getActiveType());
        couponTemplate.setGetLimit(dto.getGetLimit());
        couponTemplateMapper.insert(couponTemplate);


        CouponPublisher couponPublisher = new CouponPublisher();
        couponPublisher.setTemplateId(couponTemplate.getTemplateId());
        couponPublisher.setPublisherId(merchantId);
        couponPublisher.setShopId(shopId);
        couponPublisher.setPublisherType(dto.getPublisherType());
        couponPublisher.setCreateTime(LocalDateTime.now());
        couponPublisher.setUpdateTime(LocalDateTime.now());
        couponPublisherMapper.insert(couponPublisher);

        //大促优惠卷要发送消息队列,存入缓存
        if (dto.getActiveType() == 3) {
            couponProducer.sendPublishCouponTemplate(couponTemplate);
        }

//            死信队列

        couponProducer.sendDeleteCouponTemplate(couponTemplate);

        couponProducer.sendStartCouponTemplate(couponTemplate);


    }


  /*  @Override
    public void getCoupon(GetCouponReqDto dto) {
        String userId = SecurityUtils.getId();

//        判断优惠卷是否上架
       // '状态：0正常 1下架',
       CouponTemplate couponTemplate = couponTemplateMapper.selectById(dto.getCouponTemplateId());
       if (couponTemplate.getStatus() == 1) {
           //优惠卷未上架
           throw new BizException(ErrorCodeEnum.COUPON_NOT_ON_SALE.getCode(), ErrorCodeEnum.COUPON_NOT_ON_SALE.getMessage());
       }

        LambdaQueryWrapper<CouponReceiveRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(CouponReceiveRecord::getTemplateId, dto.getCouponTemplateId())
                .eq(CouponReceiveRecord::getUserId, userId);
        CouponReceiveRecord record = couponReceiveRecordMapper.selectOne(recordWrapper);

        if (record != null) {
            //用户已领取过优惠卷


            if (record.getStatus() == 1 || record.getReceiveCount() >= couponTemplate.getGetLimit()) {
                //已经领取了最大次数
                throw new BizException(ErrorCodeEnum.USER_ALREADY_GET_COUPON.getCode(), ErrorCodeEnum.USER_ALREADY_GET_COUPON.getMessage());
            }
            //领取优惠卷
            CouponUser couponUser = new CouponUser();
            couponUser.setTemplateId(dto.getCouponTemplateId());
            couponUser.setUserId(userId);
            couponUser.setStatus(0);
            couponUser.setStartTime(LocalDateTime.now());


            if (dto.getActiveType() == 3) {
                couponUser.setEndTime(couponTemplate.getEndTime());
            } else if (dto.getActiveType() == 2) {
                couponUser.setEndTime(LocalDateTime.now().plusDays(1));
            } else {
                couponUser.setEndTime(LocalDateTime.now().plusDays(7));
            }
            couponUser.setCreateTime(LocalDateTime.now());
            couponUser.setUpdateTime(LocalDateTime.now());
            couponUser.setExpireTime(couponUser.getEndTime().plusDays(7));
            couponUserMapper.insert(couponUser);
            //发送消息队列,更新状态
            couponProducer.sendDeleteCouponUser(couponUser);
            couponProducer.sendCouponUser(couponUser);


            //扣除优惠卷模板 ----  优惠卷数量
            CouponReceiveRecord couponReceiveRecord = new CouponReceiveRecord();
            couponReceiveRecord.setTemplateId(dto.getCouponTemplateId());
            couponReceiveRecord.setUserId(userId);

            // 第一次领取，领取次数初始化为 1
            int currentReceiveCount = 1;
            couponReceiveRecord.setReceiveCount(currentReceiveCount);

            // 判断是否已达领取上限
            if (currentReceiveCount >= couponTemplate.getGetLimit()) {
                couponReceiveRecord.setStatus(1);
            } else {
                couponReceiveRecord.setStatus(0);
            }

            couponReceiveRecord.setCreateTime(LocalDateTime.now());
            couponReceiveRecord.setUpdateTime(LocalDateTime.now());
            couponReceiveRecordMapper.insert(couponReceiveRecord);

            couponTemplate.setRemainCount(couponTemplate.getRemainCount() - 1);
            couponTemplateMapper.updateById(couponTemplate);



            record.setReceiveCount(record.getReceiveCount() + 1);
            if (record.getReceiveCount() + 1 >= couponTemplate.getGetLimit()){
                record.setStatus(1);
            }
            couponReceiveRecordMapper.updateById(record);

        }

//        第一次领取优惠卷

        //领取优惠卷
        CouponUser couponUser = new CouponUser();
        couponUser.setTemplateId(dto.getCouponTemplateId());
        couponUser.setUserId(userId);
        couponUser.setStatus(0);
        couponUser.setStartTime(LocalDateTime.now());


        if (dto.getActiveType() == 3) {
            couponUser.setEndTime(couponTemplate.getEndTime());
        } else if (dto.getActiveType() == 2) {
            couponUser.setEndTime(LocalDateTime.now().plusDays(1));
        } else {
            couponUser.setEndTime(LocalDateTime.now().plusDays(7));
        }
        couponUser.setCreateTime(LocalDateTime.now());
        couponUser.setUpdateTime(LocalDateTime.now());
        couponUser.setExpireTime(couponUser.getEndTime().plusDays(7));
        couponUserMapper.insert(couponUser);
        //发送消息队列,更新状态
        couponProducer.sendDeleteCouponUser(couponUser);
        couponProducer.sendCouponUser(couponUser);


        //扣除优惠卷模板 ----  优惠卷数量
        couponTemplate.setRemainCount(couponTemplate.getRemainCount() - 1);
        couponTemplateMapper.updateById(couponTemplate);




        CouponReceiveRecord couponReceiveRecord = new CouponReceiveRecord();
        couponReceiveRecord.setTemplateId(dto.getCouponTemplateId());
        couponReceiveRecord.setUserId(userId);

        couponReceiveRecord.setReceiveCount(couponReceiveRecord.getReceiveCount() + 1);
        if (couponReceiveRecord.getReceiveCount() + 1 >= couponTemplate.getGetLimit()){
            couponReceiveRecord.setStatus(1);
        }
        couponReceiveRecord.setStatus(0);
        couponReceiveRecord.setCreateTime(LocalDateTime.now());
        couponReceiveRecord.setUpdateTime(LocalDateTime.now());
        couponReceiveRecordMapper.insert(couponReceiveRecord);




    }

*/

    // ... 领取优惠卷 code ...
/*
    @Override
    public void receiveCoupon(receiveCouponReqDto dto) {
        String userId = SecurityUtils.getId();

//        判断优惠卷是否上架
        // '状态：0正常 1下架',
        CouponTemplate couponTemplate = couponTemplateMapper.selectById(dto.getCouponTemplateId());
        if (couponTemplate.getStatus() == 1) {
            //优惠卷未上架
            throw new BizException(ErrorCodeEnum.COUPON_NOT_ON_SALE.getCode(), ErrorCodeEnum.COUPON_NOT_ON_SALE.getMessage());
        }

        if (couponTemplate.getEndTime().isBefore(LocalDateTime.now())) {
            //优惠卷已过期
            throw new BizException(ErrorCodeEnum.COUPON_EXPIRED.getCode(), ErrorCodeEnum.COUPON_EXPIRED.getMessage());
        }
        if (couponTemplate.getRemainCount() <= 0){
            //优惠卷已售罄
            throw new BizException(ErrorCodeEnum.COUPON_SOLD_OUT.getCode(), ErrorCodeEnum.COUPON_SOLD_OUT.getMessage());
    }

        Integer activeType = couponTemplate.getActiveType();

        LambdaQueryWrapper<CouponReceiveRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(CouponReceiveRecord::getTemplateId, dto.getCouponTemplateId())
                .eq(CouponReceiveRecord::getUserId, userId);
        CouponReceiveRecord record = couponReceiveRecordMapper.selectOne(recordWrapper);

        // ✅ 第一步：先校验领取资格
        if (record != null) {
            // 用户已领取过优惠卷
            if (record.getStatus() == 1 || record.getReceiveCount() >= couponTemplate.getGetLimit()) {
                // 已经领取了最大次数，直接抛出异常，不执行后续操作
                throw new BizException(ErrorCodeEnum.USER_ALREADY_GET_COUPON.getCode(), ErrorCodeEnum.USER_ALREADY_GET_COUPON.getMessage());
            }
        }

        // ✅ 第二步：校验通过后才执行插入操作

        // 领取优惠券（创建 CouponUser）
        CouponUser couponUser = new CouponUser();
        couponUser.setTemplateId(dto.getCouponTemplateId());
        couponUser.setUserId(userId);
        couponUser.setStatus(0);
        if (couponTemplate.getStartTime().isAfter(LocalDateTime.now())) {
            //优惠卷未生效
            couponUser.setStartTime(couponTemplate.getStartTime());
        }
        else {
            //优惠卷已生效
            couponUser.setStartTime(LocalDateTime.now());
        }

        if (activeType == 3) {
            //大促活动优惠卷
            couponUser.setEndTime(couponTemplate.getEndTime());
        } else if (activeType == 2) {
            //会员优惠卷
            couponUser.setEndTime(LocalDateTime.now().plusDays(1));
        } else {
            //普通优惠卷
            couponUser.setEndTime(LocalDateTime.now().plusDays(7));
        }



        couponUser.setCreateTime(LocalDateTime.now());
        couponUser.setUpdateTime(LocalDateTime.now());

        //删除时间:优惠卷过期7天后实现删除
        couponUser.setExpireTime(couponUser.getEndTime().plusDays(7));

        couponUserMapper.insert(couponUser);


        // ✅ 第三步：更新领取记录
        if (record != null) {
            // 用户已领取过，更新领取次数
            int newReceiveCount = record.getReceiveCount() + 1;
            record.setReceiveCount(newReceiveCount);

            // 判断是否已达领取上限
            if (newReceiveCount >= couponTemplate.getGetLimit()) {
                record.setStatus(1);
            }

            couponReceiveRecordMapper.updateById(record);
        } else {
            // 第一次领取优惠卷，创建新记录
            CouponReceiveRecord newRecord = new CouponReceiveRecord();
            newRecord.setTemplateId(dto.getCouponTemplateId());
            newRecord.setUserId(userId);
            newRecord.setReceiveCount(1);

            // 判断是否已达领取上限
            if (1 >= couponTemplate.getGetLimit()) {
                newRecord.setStatus(1);
            } else {
                newRecord.setStatus(0);
            }

            newRecord.setCreateTime(LocalDateTime.now());
            newRecord.setUpdateTime(LocalDateTime.now());
            couponReceiveRecordMapper.insert(newRecord);
        }

        // 发送消息队列，更新状态
        couponProducer.sendDeleteCouponUser(couponUser);
        couponProducer.sendCouponUser(couponUser);


        // 扣除优惠卷模板 ---- 优惠卷数量
        couponTemplate.setRemainCount(couponTemplate.getRemainCount() - 1);
        couponTemplateMapper.updateById(couponTemplate);
    }

*/


/*领取优惠卷
* */
    @Override
    public String receiveCoupon(ReceiveCouponReqDto dto) {
        String userId = SecurityUtils.getId();

        CouponTemplate couponTemplate = couponTemplateMapper.selectById(dto.getCouponTemplateId());
        if (couponTemplate == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR.getCode(), "优惠券模板不存在");
        }

        if (couponTemplate.getStatus() == 1) {
            log.info("优惠券已下架");
            return "优惠券已下架";
//            throw new BizException(ErrorCodeEnum.COUPON_NOT_ON_SALE.getCode(), ErrorCodeEnum.COUPON_NOT_ON_SALE.getMessage());
        }

        if (couponTemplate.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCodeEnum.COUPON_EXPIRED.getCode(), ErrorCodeEnum.COUPON_EXPIRED.getMessage());
        }

        if (couponTemplate.getRemainCount() <= 0) {
            log.info("优惠券已售罄");
            return "优惠券已售罄";
//            throw new BizException(ErrorCodeEnum.COUPON_SOLD_OUT.getCode(), ErrorCodeEnum.COUPON_SOLD_OUT.getMessage());
        }

        Integer activeType = couponTemplate.getActiveType();

        LambdaQueryWrapper<CouponReceiveRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(CouponReceiveRecord::getTemplateId, dto.getCouponTemplateId())
                .eq(CouponReceiveRecord::getUserId, userId);
        CouponReceiveRecord record = couponReceiveRecordMapper.selectOne(recordWrapper);

        if (record != null) {
            if (record.getStatus() == 1 || record.getReceiveCount() >= couponTemplate.getGetLimit()) {
                log.info("用户已领取过优惠券");
                return "用户已领取过优惠券";
//               throw new BizException(ErrorCodeEnum.USER_ALREADY_GET_COUPON.getCode(), ErrorCodeEnum.USER_ALREADY_GET_COUPON.getMessage());
            }
        }

        CouponUser couponUser = new CouponUser();
        couponUser.setTemplateId(dto.getCouponTemplateId());
        couponUser.setUserId(userId);
        couponUser.setStatus(0);

        if (couponTemplate.getStartTime().isAfter(LocalDateTime.now())) {
            couponUser.setStartTime(couponTemplate.getStartTime());
        } else {
            couponUser.setStartTime(LocalDateTime.now());
        }

        if (activeType == 3) {
            couponUser.setEndTime(couponTemplate.getEndTime());
        } else if (activeType == 2) {
            couponUser.setEndTime(LocalDateTime.now().plusDays(1));
        } else {
            couponUser.setEndTime(LocalDateTime.now().plusDays(7));
        }

        couponUser.setCreateTime(LocalDateTime.now());
        couponUser.setUpdateTime(LocalDateTime.now());
        couponUser.setExpireTime(couponUser.getEndTime().plusDays(7));

        couponUserMapper.insert(couponUser);

        int currentReceiveCount;
        if (record != null) {
            int newReceiveCount = record.getReceiveCount() + 1;
            record.setReceiveCount(newReceiveCount);

            if (newReceiveCount >= couponTemplate.getGetLimit()) {
                record.setStatus(1);
            }

            couponReceiveRecordMapper.updateById(record);
            currentReceiveCount = newReceiveCount;
        } else {
            CouponReceiveRecord newRecord = new CouponReceiveRecord();
            newRecord.setTemplateId(dto.getCouponTemplateId());
            newRecord.setUserId(userId);
            newRecord.setReceiveCount(1);

            if (1 >= couponTemplate.getGetLimit()) {
                newRecord.setStatus(1);
            } else {
                newRecord.setStatus(0);
            }

            newRecord.setCreateTime(LocalDateTime.now());
            newRecord.setUpdateTime(LocalDateTime.now());
            couponReceiveRecordMapper.insert(newRecord);
            currentReceiveCount = 1;
        }

        couponProducer.sendDeleteCouponUser(couponUser);
        couponProducer.sendCouponUser(couponUser);

        couponTemplate.setRemainCount(couponTemplate.getRemainCount() - 1);
        couponTemplateMapper.updateById(couponTemplate);

        int remainingCount = couponTemplate.getGetLimit() - currentReceiveCount;

        if (remainingCount <= 0) {
            return "已领取！";
        } else {
            return "剩余领取次数" + remainingCount;
        }
    }

// ... existing code ...




    @Override
    public void updateCoupons(CouponUpdateReqDto dto) {

        String userId = SecurityUtils.getId();
        User user =  userMapper.selectById(userId);
        Integer userRole = user.getRole();


        CouponPublisher couponPublisher = couponPublisherMapper.selectById(dto.getCouponTemplateId());
        Integer publisherType = couponPublisher.getPublisherType();

        if (userRole != publisherType) {
            //无权修改
            throw new BizException(ErrorCodeEnum.NO_PERMISSION.getCode(), ErrorCodeEnum.NO_PERMISSION.getMessage());
        }

        CouponTemplate couponTemplate = new CouponTemplate();
        couponTemplate.setTemplateId(dto.getCouponTemplateId());
        couponTemplate.setCouponName(dto.getCouponName());
        couponTemplate.setCouponType(dto.getCouponType());
        couponTemplate.setThresholdAmount(dto.getThresholdAmount());
        couponTemplate.setDiscountAmount(dto.getDiscountAmount());
        couponTemplate.setDiscountRate(dto.getDiscountRate());
        couponTemplate.setStackable(dto.getStackable());
        couponTemplate.setStartTime(dto.getStartTime());
        couponTemplate.setEndTime(dto.getEndTime());
        couponTemplate.setTotalCount(dto.getTotalCount());
        couponTemplate.setRemainCount(dto.getTotalCount());
        couponTemplate.setStatus(dto.getStatus());
        couponTemplate.setActiveType(dto.getActiveType());
        couponTemplate.setGetLimit(dto.getGetLimit());
        couponTemplate.setCreateTime(LocalDateTime.now());
        couponTemplate.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.updateById(couponTemplate);


        //大促优惠卷要发送消息队列,存入缓存
        if (dto.getActiveType() == 3) {
            couponProducer.sendPublishCouponTemplate(couponTemplate);
        }

//            死信队列

        couponProducer.sendDeleteCouponTemplate(couponTemplate);

        couponProducer.sendStartCouponTemplate(couponTemplate);

    }


   /* @Override
    public PageResp<GetMyCouponsRespDto> getMyCoupons(GetMyCouponsReqDto dto) {
        // 1. 【安全优化】获取用户ID并增强参数健壮性
        String userId = SecurityUtils.getId();
        // 默认展示“未使用”
        Integer status = Optional.ofNullable(dto.getStatus()).orElse(0);
        // 限制每页大小，防止前端恶意传参导致内存溢出 (OOM)
        int pageSize = Math.min(dto.getPageSize(), 100);
        Page<CouponUser> pageParam = new Page<>(dto.getPageNum(), pageSize);

        // 2. 【逻辑优化】精准定义业务状态边界
        LambdaQueryWrapper<CouponUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(CouponUser::getUserId, userId);

        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case 0:
                // 未使用：必须状态为0且当前时间未过有效期
                userWrapper.eq(CouponUser::getStatus, 0)
                        .gt(CouponUser::getEndTime, now)
                        .orderByAsc(CouponUser::getEndTime);
                // 临期优先展示
                break;
            case 1:
                // 已使用
                userWrapper.eq(CouponUser::getStatus, 1)
                        .orderByDesc(CouponUser::getUsedTime);
                // 最近使用优先
                break;
            case 2:
                // 已过期：业务逻辑上包括“状态已变”或“时间已过”
                userWrapper.and(w -> w.eq(CouponUser::getStatus, 2)
                                .or(orW -> orW.eq(CouponUser::getStatus, 0)
                                        .le(CouponUser::getEndTime, now)))
                        .orderByDesc(CouponUser::getEndTime);
                // 最近过期优先
                break;
            default:
                throw new BizException("不支持的优惠券查询状态");
        }

        // 3. 【性能优化】执行查询
        IPage<CouponUser> userCouponPage = couponUserMapper.selectPage(pageParam, userWrapper);

        // 如果没有数据，返回空对象而不是 null，避免前端渲染报错
        if (userCouponPage.getRecords().isEmpty()) {
            return PageResp.of(0L, (long)pageSize, (long)dto.getPageNum(), Collections.emptyList());
        }

        // 4. 【核心优化】批量关联查询 (避免 N+1)，增强容错处理
        Set<String> templateIds = userCouponPage.getRecords().stream()
                .map(CouponUser::getTemplateId)
                .filter(Objects::nonNull)
                // 过滤掉可能的空ID
                .collect(Collectors.toSet());

        // 预留容错：如果用户有券但模板全被物理删除了
        Map<String, CouponTemplate> templateMap = templateIds.isEmpty() ? Collections.emptyMap() :
                couponTemplateMapper.selectBatchIds(templateIds).stream()
                        .collect(Collectors.toMap(CouponTemplate::getTemplateId, t -> t, (v1, v2) -> v1));

        // 5. 【规范优化】DTO 转换与字段补全
        List<GetMyCouponsRespDto> dtoList = userCouponPage.getRecords().stream().map(userCoupon -> {
            GetMyCouponsRespDto respDto = new GetMyCouponsRespDto();
            // 建议：对于复杂对象，企业级推荐使用 MapStruct，这里手动补全以保证代码自包含
            BeanUtils.copyProperties(userCoupon, respDto);

            // 处理 ID 映射：确保 couponId 取自 userCoupon 记录
            respDto.setCouponId(userCoupon.getCouponId());

            // 关联模板数据：如名称、图片、使用规则等
            CouponTemplate template = templateMap.get(userCoupon.getTemplateId());
            if (template != null) {
                respDto.setCouponName(template.getCouponName());
                // 如果未来有更多字段（如图片、金额），直接在这里 set
            } else {
                respDto.setCouponName("优惠券已失效"); // 兜底处理
            }
            return respDto;
        }).collect(Collectors.toList());

        // 6. 返回规范化的分页结果
        return PageResp.of(
                userCouponPage.getTotal(),
                userCouponPage.getSize(),
                userCouponPage.getCurrent(),
                dtoList
        );
    }
*/


// ... existing code ...
    @Override
    public PageResp<GetMyCouponsRespDto> getMyCoupons(GetMyCouponsReqDto dto) {
        // 1. 【安全优化】获取用户ID并增强参数健壮性
        String userId = SecurityUtils.getId();
        // 默认展示"未使用"
        Integer status = Optional.ofNullable(dto.getStatus()).orElse(0);
        // 限制每页大小，防止前端恶意传参导致内存溢出 (OOM)
        int pageSize = Math.min(dto.getPageSize(), 100);
        Page<CouponUser> pageParam = new Page<>(dto.getPageNum(), pageSize);

        // 2. 【逻辑优化】精准定义业务状态边界
        LambdaQueryWrapper<CouponUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(CouponUser::getUserId, userId);

        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case 0:
                // 未使用：必须状态为0且当前时间未过有效期
                userWrapper.eq(CouponUser::getStatus, 0)
                        .gt(CouponUser::getEndTime, now)
                        .orderByAsc(CouponUser::getEndTime);
                // 临期优先展示
                break;
            case 1:
                // 已使用
                userWrapper.eq(CouponUser::getStatus, 1)
                        .orderByDesc(CouponUser::getUsedTime);
                // 最近使用优先
                break;
            case 2:
                // 已过期：业务逻辑上包括"状态已变"或"时间已过"
                userWrapper.and(w -> w.eq(CouponUser::getStatus, 2)
                                .or(orW -> orW.eq(CouponUser::getStatus, 0)
                                        .le(CouponUser::getEndTime, now)))
                        .orderByDesc(CouponUser::getEndTime);
                // 最近过期优先
                break;
            default:
                throw new BizException("不支持的优惠券查询状态");
        }

        // 3. 【手动统计总数】解决分页插件 total 为 0 的问题
        long total = couponUserMapper.selectCount(userWrapper);

        // 4. 【性能优化】执行分页查询
        IPage<CouponUser> userCouponPage = couponUserMapper.selectPage(pageParam, userWrapper);

        // 如果没有数据，返回空对象而不是 null，避免前端渲染报错
        if (userCouponPage.getRecords().isEmpty()) {
            return PageResp.of(0L, (long)pageSize, (long)dto.getPageNum(), Collections.emptyList());
        }

        // 5. 【核心优化】批量关联查询 (避免 N+1)，增强容错处理
        Set<String> templateIds = userCouponPage.getRecords().stream()
                .map(CouponUser::getTemplateId)
                .filter(Objects::nonNull)
                // 过滤掉可能的空ID
                .collect(Collectors.toSet());

        // 预留容错：如果用户有券但模板全被物理删除了
        Map<String, CouponTemplate> templateMap = templateIds.isEmpty() ? Collections.emptyMap() :
                couponTemplateMapper.selectBatchIds(templateIds).stream()
                        .collect(Collectors.toMap(CouponTemplate::getTemplateId, t -> t, (v1, v2) -> v1));

        // 6. 【规范优化】DTO 转换与字段补全
        List<GetMyCouponsRespDto> dtoList = userCouponPage.getRecords().stream().map(userCoupon -> {
            GetMyCouponsRespDto respDto = new GetMyCouponsRespDto();
            // 建议：对于复杂对象，企业级推荐使用 MapStruct，这里手动补全以保证代码自包含
            BeanUtils.copyProperties(userCoupon, respDto);

            // 处理 ID 映射：确保 couponId 取自 userCoupon 记录
            respDto.setCouponId(userCoupon.getCouponId());

            // 关联模板数据：如名称、图片、使用规则等
            CouponTemplate template = templateMap.get(userCoupon.getTemplateId());
            if (template != null) {
                respDto.setCouponName(template.getCouponName());
                // 如果未来有更多字段（如图片、金额），直接在这里 set
            } else {
                respDto.setCouponName("优惠券已失效"); // 兜底处理
            }
            return respDto;
        }).collect(Collectors.toList());

        // 7. 返回规范化的分页结果（使用手动统计的 total）
        return PageResp.of(
                total,
                (long)pageSize,
                (long)dto.getPageNum(),
                dtoList
        );
    }

    @Transactional
    public boolean useCouponsToRecord(String userId, List<String> couponIds, String orderId) {
        for (String couponId : couponIds) {
            // 1. 原子更新优惠券使用次数，利用数据库行锁（乐观锁通过版本号或条件更新）
            int updated = couponUserMapper.markAsUsed(userId, couponId);
            if (updated != 1) {
                log.warn("优惠券核销失败，可能已用完或已过期: couponId={}", couponId);
                throw new BizException("优惠券 " + couponId + " 不可用");
            }

            // 2. 插入使用记录（利用唯一索引防重）
            CouponUsedRecord record = new CouponUsedRecord();
            record.setCouponId(couponId);
            record.setUserId(userId);
            record.setOrderId(orderId);
            record.setStatus(1);
            record.setUsedTime(LocalDateTime.now());
            try {
                couponUsedRecordMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // 如果重复插入，说明该券已被当前订单使用过（理论上不会发生，但防止并发）
                throw new BizException("优惠券已被使用");
            }
        }
        return true;
    }

}
