package com.mall.demo.module.payment.service;


import com.mall.demo.module.payment.entity.Payment;


public interface PaymentNotifyMessageService {

    /*
    * 通知发货人发货
    * */
    /**
     * 通知商户发货
     * @param paymentId 支付单ID
     * @param suborderId 子订单ID
     * @param merchantId 商户ID
     */
    void notifyMerchantForShip(String paymentId, String suborderId, String merchantId);




    /*
    * 通知用户---用户支付信息
    * @param paymentId 支付单ID
    * @param userId 用户ID
    * @param orderId 订单ID
    * @param suborderId_JSON 子订单ID_JSON
    * */
    void notifyUserForPayment(String paymentId, String userId, String orderId, String suborderId_JSON);



    void testPushToUser(String userId);





}
