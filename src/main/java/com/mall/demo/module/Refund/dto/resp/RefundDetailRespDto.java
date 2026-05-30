package com.mall.demo.module.Refund.dto.resp;

import lombok.Data;

@Data
public class RefundDetailRespDto {


    //退款信息
    String refundReason;
    String refundAmount;
    String status;
    String refundId;
    String orderId;
    String suborderId;
    String userId;

    //商品信息
    String productName;
    String spec;
    String productUrl;


    //收货人信息
    String receiverPhone;
    String receiverName;
    String address;



}
