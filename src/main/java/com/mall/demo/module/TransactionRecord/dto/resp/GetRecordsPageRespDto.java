package com.mall.demo.module.TransactionRecord.dto.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GetRecordsPageRespDto {

    private String transactionId;
    private String orderId;
    private String payType;
    private BigDecimal payAmount;
    private String productNameText;
    private LocalDateTime payTime;
    private Integer transactionStatus;



}
