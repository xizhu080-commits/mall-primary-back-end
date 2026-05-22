package com.mall.demo.module.payment.dto.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayRespDto {

    private   String orderId;
   private String userId;
   private String payType;
   private BigDecimal payAmount;


    private String paymentId;
    private String qrCode;
    private LocalDateTime payTime;
}
