package com.mall.demo.module.Refund.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HandleRefundRespDto {


    private String merchantId;


    private String suborderId;
    private String orderId;


    //退款记录id
    private String refundId;
    //处理结果
    private Integer status;

    //拒绝退款理由
    private String rejectReason;
    //处理时间
    private LocalDateTime handleTime;
}
