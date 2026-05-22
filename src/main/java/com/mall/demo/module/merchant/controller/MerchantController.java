package com.mall.demo.module.merchant.controller;

import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.merchant.dto.req.MerchantLoginReqDto;
import com.mall.demo.module.merchant.dto.req.MerchantRegisterReqDto;
import com.mall.demo.module.merchant.dto.req.MerchantResetPasswordReqDto;
import com.mall.demo.module.merchant.dto.req.MerchantUpdateReqDto;
import com.mall.demo.module.merchant.dto.resp.GetMerchantInfoRespDto;
import com.mall.demo.module.merchant.dto.resp.MerchantLoginRespDto;
import com.mall.demo.module.merchant.service.MerchantService;
import com.mall.demo.module.user.dto.req.UserResetPasswordReqDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@Tag(name = "商家管理", description = "商家入驻、登录、信息管理接口")
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 发送验证码
     */
    @Operation(summary = "发送验证码")
    @GetMapping("/sendCode")
    public RestResp<String> sendCode(@RequestParam String phone) {
        String result = merchantService.sendCode(phone);
        return RestResp.ok(result);
    }

    /**
     * 商家入驻（注册）
     */
    @Operation(summary = "商家入驻")
    @PostMapping("/register")
    public RestResp<String> register(@Valid @RequestBody MerchantRegisterReqDto dto) {
        merchantService.register(dto);
        return RestResp.ok("商家入驻成功");
    }

    /**
     * 商家登录
     */
    @Operation(summary = "商家登录")
    @PostMapping("/login")
    public RestResp<MerchantLoginRespDto> login(@Valid @RequestBody MerchantLoginReqDto dto) {
        log.info("前端传入的商家ID:{}",dto.getMerchantId());
        log.info("前端传入的商家手机号:{}",dto.getPhone());
        log.info("前端传入的商家密码:{}",dto.getPassword());
        log.info("前端传入的验证码:{}",dto.getCode());

        return RestResp.ok( merchantService.login(dto));
    }

    /**
     * 获取当前登录商家信息
     */
    @Operation(summary = "获取当前商家信息")
    @GetMapping("/info")
    public RestResp<GetMerchantInfoRespDto> getCurrentMerchantInfo() {

        try {
            GetMerchantInfoRespDto result = merchantService.getMerchantInfo();
            return RestResp.ok(result);
        } catch (BizException e) {
            log.error("获取商家信息失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 修改商家信息
     */
    @Operation(summary = "修改商家信息")
    @PutMapping("/update")
    public RestResp<String> updateMerchant(@Valid @RequestBody MerchantUpdateReqDto dto) {
        merchantService.updateMerchant(dto);
        return RestResp.ok("修改成功");
    }

    /**
     * 商家注销（退店）
     */
    @Operation(summary = "商家注销")
    @DeleteMapping("/resign")
    public RestResp<String> resign(HttpServletRequest request) {
        // 从请求头中获取token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        merchantService.resign(token);
        return RestResp.ok("注销成功");
    }








    @PostMapping("/resetPassword")
    @Operation(summary = "重置密码", description = "通过手机号和验证码重置密码")
    public RestResp<Void> merchantResetPassword(@Valid @RequestBody MerchantResetPasswordReqDto dto) {
        log.info("前端传入的商家ID:{}",dto.getMerchantId());
        log.info("前端传入的商家手机号:{}",dto.getPhone());
        log.info("前端传入的商家密码:{}",dto.getPassword());
        log.info("前端传入的验证码:{}",dto.getCode());

        return RestResp.ok( merchantService.merchantResetPassword(dto),null);
    }











}