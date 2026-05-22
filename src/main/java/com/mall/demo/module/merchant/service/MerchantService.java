package com.mall.demo.module.merchant.service;

import com.mall.demo.module.merchant.dto.req.MerchantLoginReqDto;
import com.mall.demo.module.merchant.dto.req.MerchantRegisterReqDto;
import com.mall.demo.module.merchant.dto.req.MerchantResetPasswordReqDto;
import com.mall.demo.module.merchant.dto.req.MerchantUpdateReqDto;
import com.mall.demo.module.merchant.dto.resp.GetMerchantInfoRespDto;
import com.mall.demo.module.merchant.dto.resp.MerchantLoginRespDto;
import com.mall.demo.module.merchant.entity.Merchant;
import com.mall.demo.module.user.dto.req.UserResetPasswordReqDto;

public interface MerchantService {

    /**
     * 商家入驻（包含自动创建默认店铺）
     */
    void register(MerchantRegisterReqDto dto);

    /*
    * 登录
    * */
    MerchantLoginRespDto login(MerchantLoginReqDto dto);



    /**
     * 修改商家资料
     */
    void updateMerchant(MerchantUpdateReqDto dto);

    /**
     * 注销商家身份
     */
    void resign(String token);


    Merchant getMerchantByPhone(String phone);
    Merchant getMerchantByMerchantId(String merchantId);


    //  发送验证码
    String sendCode(String phone);


    // 验证验证码
    String checkCode(String phone, String code);





    String merchantResetPassword(MerchantResetPasswordReqDto dto);

// 获取当前登录用户的商家资料
    GetMerchantInfoRespDto getMerchantInfo();





}
