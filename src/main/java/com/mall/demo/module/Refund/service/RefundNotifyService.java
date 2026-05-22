package com.mall.demo.module.Refund.service;


import com.mall.demo.module.Refund.entity.Refund;
import com.mall.demo.module.order.entity.Suborder;

public interface RefundNotifyService {

    //用户申请退款 → 通知商家
    void notifyMerchantForApply(Refund refund, Suborder suborder, String content,String title);


    //商家处理退款 → 通知用户
    void notifyUserForHandle(Refund refund, Boolean agreed, String rejectReason, String content,String title);




}
