package com.mall.demo.module.Refund.dto.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ApplyRefundRespDto {



    //退款用户
    private String userId;

    private String suborderId;
    private String orderId;



    private BigDecimal refundAmount;

    private String refundReason;

    private String refundId;
    private LocalDateTime applyRefundTime;
}
