package com.mall.demo.module.coupon.dto.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AmountPayableRespDto {



        // 原始总金额
        private BigDecimal totalAmount;

        // 优惠金额
        private BigDecimal discountAmount;

        // 实付金额
        private BigDecimal payableAmount;

        // 使用的优惠券ID集合
        private List<String> couponIds;

}
