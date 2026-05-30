package com.mall.demo.module.payment.service;


import com.mall.demo.module.payment.dto.req.PayReqDto;
import com.mall.demo.module.payment.dto.resp.PayRespDto;

import java.util.Map;

public interface PaymentService {




    /*
    * 支付订单
    * */
    PayRespDto createPayment(PayReqDto dto);


    /*
    * 支付回调
    * */
    String processAlipayNotify(Map<String,String> params);




}
