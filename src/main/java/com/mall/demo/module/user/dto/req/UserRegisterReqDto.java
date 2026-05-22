package com.mall.demo.module.user.dto.req;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
public class UserRegisterReqDto {
/*
* 注册请求dto
*
* */

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "确认密码")
    private String confirmPassword;


    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码")
    private String code;


    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
