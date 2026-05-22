package com.mall.demo.module.user.service;

import com.mall.demo.module.user.dto.req.UserResetPasswordReqDto;
import com.mall.demo.module.user.dto.req.UserLoginReqDto;
import com.mall.demo.module.user.dto.req.UserRegisterReqDto;
import com.mall.demo.module.user.dto.req.UserUpdateReqDto;
import com.mall.demo.module.user.dto.resp.GetUserInfoRespDto;
import com.mall.demo.module.user.dto.resp.UserLoginRespDto;
import com.mall.demo.module.user.entity.User;

public interface UserService {

    //    注册
    String register(UserRegisterReqDto dto);

    //    登录
    UserLoginRespDto login(UserLoginReqDto dto);

    //    更新
    void update(UserUpdateReqDto dto);


    String resetPassword(UserResetPasswordReqDto dto);


    //    获取当前用户信息
    GetUserInfoRespDto getCurrentUserInfo();

    //  发送验证码
    String sendCode(String phone);


    // 验证验证码
    String checkCode(String phone, String code);


    //    Spring Security 验证用户是否存在
    User getUserByPhone(String phone);

    User getUserByUserId(String userId);
}
