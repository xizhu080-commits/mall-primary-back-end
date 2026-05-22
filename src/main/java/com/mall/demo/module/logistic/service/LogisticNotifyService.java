package com.mall.demo.module.logistic.service;

import com.mall.demo.module.logistic.entity.Logistic;

public interface LogisticNotifyService {

    //   物流通知:   物流到达      ---->   通知用户签收确认
    void notifyUserForSign(String logisticId, String consigneeId, String content);




    //   物流通知:   用户处理物流      ---->   通知商家
    void notifyMerchantForHandle(Logistic logistic, Boolean agreed, String rejectReason,String content);


    //    物流通知:  商家处理物流      ---->   通知用户
    void notifyUserForHandle(Logistic logistic,String consigneeId, String content);



}
