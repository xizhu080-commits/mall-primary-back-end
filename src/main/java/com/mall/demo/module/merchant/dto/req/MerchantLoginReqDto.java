package com.mall.demo.module.merchant.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantLoginReqDto {


    @NotBlank(message = "商家ID不能为空")
    private String merchantId;

    @NotBlank(message = "手机号不能为空")
    private String phone;
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "验证码不能为空")
    private String code;

}
