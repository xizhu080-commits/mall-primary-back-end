package com.mall.demo.module.user.controller;


import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.user.dto.req.UserResetPasswordReqDto;
import com.mall.demo.module.user.dto.req.UserLoginReqDto;
import com.mall.demo.module.user.dto.req.UserRegisterReqDto;
import com.mall.demo.module.user.dto.req.UserUpdateReqDto;
import com.mall.demo.module.user.dto.resp.GetUserInfoRespDto;
import com.mall.demo.module.user.dto.resp.UserLoginRespDto;
import com.mall.demo.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /*
     * 注册
     *
     * */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "通过手机号注册用户，保证手机号唯一性")
    public RestResp<String> register(@Valid @RequestBody UserRegisterReqDto dto) {

        return RestResp.ok(userService.register(dto));
    }

    /*
     * 获取验证码
     * */
    @GetMapping("/sendCode")
    @Operation(summary = "获取验证码", description = "通过手机号获取验证码")
    public String sendCode(@RequestParam String phone) {
        log.info("前端传入的手机号：{}", phone);
        return userService.sendCode(phone);
    }




    /*
    *
    * 登录
    *
    * */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过手机号和密码登录用户")
    public RestResp<UserLoginRespDto> login(@Valid @RequestBody UserLoginReqDto dto) {
        log.info("前端传入的用户ID：{}", dto.getUserId());
        log.info("前端传入的手机号：{}", dto.getPhone());
        log.info("前端传入的验证码：{}", dto.getCode());
        return RestResp.ok(userService.login(dto));
    }

    /*
    * 修改个人信息(用户名、头像、性别)
    * */
    @PatchMapping("/update")
    @Operation(summary = "修改个人信息", description = "通过token修改用户信息")
    public RestResp<Void> update(@Valid @RequestBody UserUpdateReqDto dto) {

        userService.update(dto);
        return RestResp.ok("更新成功!",null);
    }



    /*
    * 获取当前用户信息
    *
    * */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "通过token获取当前用户信息")
    public RestResp<GetUserInfoRespDto> getCurrentUserInfo() {

        return RestResp.ok(userService.getCurrentUserInfo());
    }


    @PostMapping("/resetPassword")
    @Operation(summary = "重置密码", description = "通过手机号和验证码重置密码")
    public RestResp<Void> userResetPassword(@Valid @RequestBody UserResetPasswordReqDto dto) {
        log.info("前端传入的用户ID：{}", dto.getUserId());
        log.info("前端传入的手机号：{}", dto.getPhone());
        log.info("前端传入的验证码：{}", dto.getCode());
      return RestResp.ok( userService.resetPassword(dto),null);
    }

}
