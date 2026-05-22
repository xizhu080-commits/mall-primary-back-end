package com.mall.demo.module.Refund.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplyRefundReqDto {


    //用户申请退款参数

    private String suborderId;
    private String orderId;
    private String refundReason;

}
