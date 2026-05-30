package com.mall.demo.module.merchant.service;

import cn.hutool.core.util.DesensitizedUtil;
import com.alibaba.fastjson2.JSON;
import com.alipay.api.domain.Coupon;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.redis.RedisService;
import com.mall.demo.common.util.JwtUtils;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.Refund.entity.RefundNotifyMessage;
import com.mall.demo.module.Refund.mapper.RefundNotifyMessageMapper;
import com.mall.demo.module.coupon.entity.CouponPublisher;
import com.mall.demo.module.coupon.entity.CouponTemplate;
import com.mall.demo.module.coupon.mapper.CouponPublisherMapper;
import com.mall.demo.module.coupon.mapper.CouponTemplateMapper;
import com.mall.demo.module.logistic.entity.LogisticNotifyMessage;
import com.mall.demo.module.logistic.mapper.LogisticNotifyMessageMapper;
import com.mall.demo.module.merchant.dto.req.MerchantLoginReqDto;
import com.mall.demo.module.merchant.dto.req.MerchantRegisterReqDto;
import com.mall.demo.module.merchant.dto.req.MerchantResetPasswordReqDto;
import com.mall.demo.module.merchant.dto.req.MerchantUpdateReqDto;
import com.mall.demo.module.merchant.dto.resp.GetMerchantInfoRespDto;
import com.mall.demo.module.merchant.dto.resp.MerchantLoginRespDto;
import com.mall.demo.module.merchant.entity.Merchant;
import com.mall.demo.module.merchant.mapper.MerchantMapper;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.module.payment.entity.PaymentNotifyMessage;
import com.mall.demo.module.payment.mapper.PaymentNotifyMessageMapper;
import com.mall.demo.module.product.entity.SKU;
import com.mall.demo.module.product.mapper.SKUMapper;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import com.mall.demo.module.user.dto.req.UserResetPasswordReqDto;
import com.mall.demo.module.user.dto.resp.GetUserInfoRespDto;
import com.mall.demo.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service

@RequiredArgsConstructor
public final class MerchantServiceImpl implements MerchantService {


    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;
    private final SKUMapper skuMapper;
    private final CouponPublisherMapper couponPublisherMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final PaymentNotifyMessageMapper paymentNotifyMessageMapper;
    private final RefundNotifyMessageMapper refundNotifyMessageMapper;
    private final LogisticNotifyMessageMapper logisticNotifyMessageMapper;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisService redisService;
    private final SuborderMapper suborderMapper;
    private final JwtUtils jwtUtils;

    private final PasswordEncoder passwordEncoder;





    @Override
    public void register(MerchantRegisterReqDto dto) {

        String phone = dto.getPhone();
        String password = dto.getPassword();
        String confirmPassword = dto.getConfirmPassword();

//       1. 校验手机号
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
        }

//       2.校验密码
        //格式
        if (password == null || password.length() < 6) {
            throw new BizException(ErrorCodeEnum.PASSWORD_NOT_EXIST.getCode(), ErrorCodeEnum.PASSWORD_NOT_EXIST.getMessage());
        }
        //确认密码是否一致
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            throw new BizException(ErrorCodeEnum.PASSWORD_NOT_MATCH.getCode(), ErrorCodeEnum.PASSWORD_NOT_MATCH.getMessage());
        }


//        3.校验验证码
        checkCode(phone, dto.getCode());



        String realName = dto.getRealName();
        String idCard = dto.getIdCard();
        String qualificationCertificate = dto.getQualificationCertificate();
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getPhone, phone)
                .eq(Merchant::getRealName, realName)
                .eq(Merchant::getQualificationCertificate, qualificationCertificate)
                .eq(Merchant::getIdCard, idCard);
        Merchant merchant = merchantMapper.selectOne(wrapper);
        if (merchant != null) {
            throw new BizException(ErrorCodeEnum.MERCHANT_EXIST.getCode(), ErrorCodeEnum.MERCHANT_EXIST.getMessage());
        }


        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
        }


        String encodedPassword = passwordEncoder.encode(password);

        merchant = new Merchant();
        merchant.setPhone(dto.getPhone());
        merchant.setRealName(dto.getRealName());
        merchant.setMerchantName(dto.getMerchantName());
        merchant.setIdCard(dto.getIdCard());
        merchant.setQualificationCertificate(dto.getQualificationCertificate());
        merchant.setIntro(dto.getIntro());
        merchant.setMerchantStatus(1);
        merchant.setPassword(encodedPassword);
        merchant.setUpdateTime(LocalDateTime.now());
        merchant.setCreateTime(LocalDateTime.now());


//        插入数据库
        try {
            log.info("尝试插入数据库");
            merchantMapper.insert( merchant);
            log.info("插入数据库成功!!!");
            log.info("注册成功");
        } catch (DuplicateKeyException e) {
            log.error("数据库插入冲突详细:", e);
            log.error("手机号:{} 已注册,插入数据库失败", phone);
            throw new BizException(ErrorCodeEnum.PHONE_EXIST.getCode(), ErrorCodeEnum.PHONE_EXIST.getMessage());
        }




    }





    @Override
    public void updateMerchant(MerchantUpdateReqDto dto) {

        String merchantId = SecurityUtils.getId();

        if (merchantId == null) {
            throw new BizException(ErrorCodeEnum.MERCHANT_NOT_EXIST.getCode(), ErrorCodeEnum.MERCHANT_NOT_EXIST.getMessage());
        }

        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(ErrorCodeEnum.MERCHANT_NOT_EXIST.getCode(), ErrorCodeEnum.MERCHANT_NOT_EXIST.getMessage());
        }

        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (!dto.getPhone().matches("^1[3-9]\\d{9}$")) {
               throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
            }
            merchant.setPhone(dto.getPhone());
        }

        if (dto.getIntro() != null) {
            if (dto.getIntro().length() > 200) {
                throw new BizException(ErrorCodeEnum.INTRO_ERROR.getCode(), ErrorCodeEnum.INTRO_ERROR.getMessage());
            }
            merchant.setIntro(dto.getIntro());
        }

        merchant.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(merchant);

        String key = "merchant:" + merchantId;
        redisService.delete(key);
        log.info("更新成功");

    }



    @Override
    public void resign(String token) {
        String merchantId = SecurityUtils.getId();

        if (merchantId == null) {
            throw new BizException(ErrorCodeEnum.MERCHANT_NOT_EXIST.getCode(), ErrorCodeEnum.MERCHANT_NOT_EXIST.getMessage());
        }

        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(ErrorCodeEnum.MERCHANT_NOT_EXIST.getCode(), ErrorCodeEnum.MERCHANT_NOT_EXIST.getMessage());
        }

        merchant.setMerchantStatus(0);
        merchant.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(merchant);

        String key = "merchant:" + merchantId;
        redisService.delete(key);

        stringRedisTemplate.delete("token:" + token);
        log.info("注销成功");
    }



    @Override
    public Merchant getMerchantByPhone(String phone){


        String key = "cache:merchant:" + phone;

        String json = stringRedisTemplate.opsForValue().get(key);

        if (json != null) {
            if ("NULL".equals(json)) {
                return null;
            }
            return JSON.parseObject(json, Merchant.class);
        }

        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>().eq(Merchant::getPhone, phone));

        if (merchant == null) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    "NULL",
                    2,
                    TimeUnit.MINUTES
            );
            return null;
        }

        stringRedisTemplate.opsForValue().set(
                key,
                JSON.toJSONString(merchant),
                30,
                TimeUnit.MINUTES
        );

        return merchant;
    }

    @Override
    public Merchant getMerchantByMerchantId(String merchantId) {
        String key = "cache:merchant:" + merchantId;

        String json = stringRedisTemplate.opsForValue().get(key);

        if (json != null) {
            if ("NULL".equals(json)) {
                return null;
            }
            return JSON.parseObject(json, Merchant.class);
        }

        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>().eq(Merchant::getMerchantId, merchantId));

        if (merchant == null) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    "NULL",
                    2,
                    TimeUnit.MINUTES
            );
            return null;
        }

        stringRedisTemplate.opsForValue().set(
                key,
                JSON.toJSONString(merchant),
                30,
                TimeUnit.MINUTES
        );

        return merchant;
    }



    @Override
    public MerchantLoginRespDto login(MerchantLoginReqDto dto) {
        String phone = dto.getPhone();
        String merchantId = dto.getMerchantId();
        // 防刷 - 使用正确的key前缀
        String rateLimitKey = "login:rate:" + phone;

        Long keyCount = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        log.info("【防刷】=访问次数:{}", keyCount);
        if (keyCount == 1) {
            stringRedisTemplate.expire(rateLimitKey, 60, TimeUnit.SECONDS);
        }
        if (keyCount > 10) {
            throw new BizException(ErrorCodeEnum.MANY_REQUEST.getCode(), ErrorCodeEnum.MANY_REQUEST.getMessage());
        }
        log.info("【防刷】客户端IP:{} 访问成功", rateLimitKey);


//查数据库

        LambdaQueryWrapper<Merchant> merchantWrapper = new LambdaQueryWrapper<>();
        merchantWrapper.eq(Merchant::getPhone, phone);
        Merchant merchant = merchantMapper.selectOne(merchantWrapper);
        if (merchant == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }

        // 校验密码
        log.info("【用户中心】开始校验密码...");
        if (!passwordEncoder.matches(dto.getPassword(), merchant.getPassword())) {
            throw new BizException(ErrorCodeEnum.PASSWORD_ERROR.getCode(), ErrorCodeEnum.PASSWORD_ERROR.getMessage());
        }
        log.info("【用户中心】密码校验成功...");
        log.info("【用户中心】开始校验验证码...");
        checkCode(phone, dto.getCode());
        log.info("用户{}登陆成功", dto.getPhone());


        // ... existing code ...
        // 3. 删除该商户的旧 token（实现单点登录）
        String oldTokenKey = "merchantToken:" + merchant.getMerchantId();
        String oldToken = stringRedisTemplate.opsForValue().get(oldTokenKey);
        if (oldToken != null) {
            // 删除旧的 token -> merchantId 映射
            stringRedisTemplate.delete("token:" + oldToken);
            log.info("【缓存中心】删除商户 {} 的旧 token", merchant.getMerchantId());
        }

        // 生成token
        log.info("【商户中心】开始生成token...");
        String token = jwtUtils.generateToken(merchantId, "MERCHANT");

        // 6. 存 Redis（token）
        // 存储 merchantToken:{merchantId} -> token 映射（用于单点登录）
        stringRedisTemplate.opsForValue().set(
                "merchantToken:" + merchant.getMerchantId(),
                token,
                7,
                TimeUnit.DAYS
        );

        // 存储 token:{token} -> merchantId 映射（用于验证）
        stringRedisTemplate.opsForValue().set(
                "token:" + token,
                merchantId,
                7,
                TimeUnit.DAYS
        );
// ... existing code ...




        log.info("【用户中心】生成token成功:{}", token);
        log.info("【缓存中心】存储用户信息...");
        redisService.set("merchant:" + merchantId, JSON.toJSONString(merchant), 10);
        log.info("商家登陆成功");

        MerchantLoginRespDto merchantLoginRespDto = new MerchantLoginRespDto();
        merchantLoginRespDto.setToken(token);
        merchantLoginRespDto.setMerchantId(merchantId);
        merchantLoginRespDto.setMerchantName(merchant.getMerchantName());
        merchantLoginRespDto.setPhone(merchant.getPhone());
        merchantLoginRespDto.setRole(merchant.getRole());
        return merchantLoginRespDto;
    }

    @Override
//    发送验证码
    public String sendCode(String phone) {

//        校验手机号
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
        }

        // 1. 限流（60秒内只能发一次）
        String key = "code:send:" + phone;
        if (stringRedisTemplate.hasKey(key)) {
            throw new BizException(ErrorCodeEnum.MANY_REQUEST.getCode(), ErrorCodeEnum.MANY_REQUEST.getMessage());
        }

        // 2. 生成验证码（6位）
        String code = String.valueOf(
                ThreadLocalRandom.current().nextInt(100000, 999999)
        );

        // 3. 存 Redis（验证码）
        stringRedisTemplate.opsForValue().set(
                "code:" + phone,
                code,
                3,
                TimeUnit.MINUTES
        );

        // 4. 设置发送限制（60秒）
        stringRedisTemplate.opsForValue().set(
                key,
                "1",
                60,
                TimeUnit.SECONDS
        );

        // 5. 调用短信服务（伪代码）
        //smsService.send(phone, code);
        log.info("发送验证码到手机: {}, 验证码: {}", phone, code);


        return "手机号码:" + phone + " 发送成功,验证码有效期3分钟";
    }

    @Override
//    校验验证码
    public String checkCode(String phone, String code) {

        /*
         * phone 前端传入的手机号 ,根据该手机号查询缓存获取验证码
         * code  前端传入的验证码,判断该验证码与缓存的验证码是否一致
         * cacheCode 缓存的验证码
         * */


        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
        }

        // 1. 从 Redis 获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get("code:" + phone);
        if (cacheCode == null) {
            throw new BizException(ErrorCodeEnum.CODE_EXPIRED.getCode(), ErrorCodeEnum.CODE_EXPIRED.getMessage());
        }

        if (!cacheCode.equals(code)) {
            throw new BizException(ErrorCodeEnum.CODE_ERROR.getCode(), ErrorCodeEnum.CODE_ERROR.getMessage());
        }
        // 2. 删除验证码缓存
        stringRedisTemplate.delete("code:" + phone);

        return "校验成功";
    }

    @Override
    public String merchantResetPassword(MerchantResetPasswordReqDto dto) {


            String merchantId = dto.getMerchantId() ;
            String phone = dto.getPhone();
            String password = dto.getPassword();
            String confirmPassword = dto.getConfirmPassword();



            Merchant merchant = merchantMapper.selectById(merchantId);
            if (merchant == null){
                throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
            }

//       1. 校验手机号
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                throw new BizException(ErrorCodeEnum.PHONE_ERROR.getCode(), ErrorCodeEnum.PHONE_ERROR.getMessage());
            }

//       2.校验密码
            //格式
            if (password == null || password.length() < 6) {
                throw new BizException(ErrorCodeEnum.PASSWORD_NOT_EXIST.getCode(), ErrorCodeEnum.PASSWORD_NOT_EXIST.getMessage());
            }
            //确认密码是否一致
            if (confirmPassword == null || !confirmPassword.equals(password)) {
                throw new BizException(ErrorCodeEnum.PASSWORD_NOT_MATCH.getCode(), ErrorCodeEnum.PASSWORD_NOT_MATCH.getMessage());
            }


//        3.校验验证码
            checkCode(phone, dto.getCode());





//        密码加密Bcrypt
            String encodedPassword = passwordEncoder.encode(password);


            merchant.setPassword(encodedPassword);
            merchant.setUpdateTime(LocalDateTime.now());

//        插入数据库
            try {

                merchantMapper.updateById(merchant);

            } catch (DuplicateKeyException e) {

                throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), ErrorCodeEnum.USER_NOT_EXIST.getMessage());
            }


            return "商户密码重置成功!";
        }










//获取商户信息
    @Override
    public GetMerchantInfoRespDto getMerchantInfo() {
        // 1. 从 SecurityUtils 获取当前用户ID
        String merchantId = SecurityUtils.getId();
        log.info("当前商家的Id:{}", merchantId);

        // 2. 缓存Key
        String cacheKey = "merchant:" + merchantId;

        // 3. 使用通用缓存方法
        Merchant merchant = redisService.get(
                cacheKey,
                Merchant.class,
                (key) -> {
                    // 从数据库查询用户
                    return merchantMapper.selectById(merchantId);
                },
                30
                // 缓存30分钟
        );

        // 4. 判断用户是否存在
        if (merchant == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), "用户不存在");
        }



        //子订单数量
        Integer suborderCount = suborderMapper.getSuborderCountById(merchantId);

        Integer messageCount = suborderMapper.getMessageCountById(merchantId);


        //店铺数量
        Integer shopCount = merchantMapper.getMerchantShopCount(merchantId);
        //商品数量
        Integer skuCount = 0;



        //获取商家最近一个月的订单
        List<Suborder> latestSuborderList = merchantMapper.getMerchantLatestSuborderList(merchantId);
        //获取商家全部订单
        List<Suborder> allSuborderList = merchantMapper.getMerchantAllSuborderList(merchantId);
        //获取商家全部店铺
        LambdaQueryWrapper<Shop> shopWrapper = new LambdaQueryWrapper<>();
        shopWrapper.eq(Shop::getMerchantId, merchantId);
        List<Shop> myShopList = shopMapper.selectList(shopWrapper);
        //获取店铺全部商品
        List<SKU> mySkuList = new ArrayList<>();
        for (Shop shop : myShopList) {
            String shopId = shop.getShopId();
            LambdaQueryWrapper<SKU> skuWrapper = new LambdaQueryWrapper<>();
            skuWrapper.eq(SKU::getShopId, shopId);
             mySkuList = skuMapper.selectList(skuWrapper);
            skuCount += merchantMapper.getMerchantSkuCount(shopId);
        }
        //获取发布的优惠卷
        LambdaQueryWrapper<CouponPublisher> couponWrapper = new LambdaQueryWrapper<>();
        couponWrapper.eq(CouponPublisher::getPublisherId, merchantId);
        List<CouponPublisher> myCouponList = couponPublisherMapper.selectList(couponWrapper);
        List<CouponTemplate> couponTemplateList = new ArrayList<>();
        for (CouponPublisher coupon : myCouponList){
            String couponTemplateId = coupon.getTemplateId();
            CouponTemplate couponTemplate = couponTemplateMapper.selectById(couponTemplateId);
            couponTemplateList.add(couponTemplate);
        }







        //我的消息:退款消息
        LambdaQueryWrapper<RefundNotifyMessage> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.eq(RefundNotifyMessage::getTargetUserId, merchantId);
         List<RefundNotifyMessage> myRefundMessageList = refundNotifyMessageMapper.selectList(refundWrapper);
        //我的消息:物流消息
        LambdaQueryWrapper<LogisticNotifyMessage> logisticWrapper = new LambdaQueryWrapper<>();
        logisticWrapper.eq(LogisticNotifyMessage::getTargetUserId, merchantId);
         List<LogisticNotifyMessage> myLogisticMessageList = logisticNotifyMessageMapper.selectList(logisticWrapper);
        //我的消息:支付消息
        LambdaQueryWrapper<PaymentNotifyMessage> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(PaymentNotifyMessage::getTargetUserId, merchantId);
         List<PaymentNotifyMessage> myPayMessageList = paymentNotifyMessageMapper.selectList(paymentWrapper);













        //返回
        GetMerchantInfoRespDto respDto = new GetMerchantInfoRespDto();
        respDto.setMerchantId(merchant.getMerchantId());
        respDto.setMerchantAvatar(merchant.getMerchantAvatar());
        respDto .setMerchantName(merchant.getMerchantName());
        String maskedPhone = DesensitizedUtil.mobilePhone(merchant.getPhone());
        respDto.setPhone(maskedPhone);
        respDto.setRealName(merchant.getRealName());
        respDto.setIdCard(merchant.getIdCard());
        respDto.setQualificationCertificate(merchant.getQualificationCertificate());
        respDto.setIntro(merchant.getIntro());
        respDto.setMerchantStatus(merchant.getMerchantStatus());
        respDto.setBrand(merchant.getBrand());
        respDto.setCreateTime(merchant.getCreateTime());
        //订单数
        respDto.setSuborderCount(suborderCount);
        //站内信数
        respDto.setMessageCount(messageCount);


        //商品数量
        respDto.setSkuCount(skuCount);
        //店铺数量
        respDto.setShopCount(shopCount);


        //我的角色
        respDto.setRole(merchant.getRole());
        //最小订单
        respDto.setLatestSuborderList(latestSuborderList);
        //全部订单
        respDto.setAllSuborderList(allSuborderList);
        //全部店铺
        respDto.setMyShopList(myShopList);
        //全部商品
        respDto.setMySkuList(mySkuList);
        //全部优惠卷
        respDto.setMyCouponList(myCouponList);
        //全部优惠卷模板
        respDto.setMyCouponTemplateList(couponTemplateList);
        //我的消息:物流消息
        respDto.setMyRefundMessageList(myRefundMessageList);
        //我的消息:支付消息
        respDto.setMyPayMessageList(myPayMessageList);
        //我的消息:退款消息
        respDto.setMyLogisticMessageList(myLogisticMessageList);



        // 根据您的 GetUserInfoRespDto 字段继续设置
        return respDto;
    }




}
