package com.mall.demo.module.merchant.dto.resp;

import lombok.Data;

@Data
public class MerchantLoginRespDto {

    private String token;

    private String merchantId;

    private String merchantName;
    private String phone;

    private Integer role;
}
