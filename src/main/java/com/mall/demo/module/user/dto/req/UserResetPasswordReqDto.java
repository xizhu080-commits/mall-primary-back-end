package com.mall.demo.module.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserResetPasswordReqDto {

    @NotNull(message = "用户id不能为空")
    @Schema(description = "用户id")
    private String userId;



    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;
    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码")
    private String confirmPassword;


    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码")
    private String code;



}
