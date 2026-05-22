package com.mall.demo.module.wallet.dto.req;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeBalanceReqDto {
    // 充值金额
    private BigDecimal amount;
}
