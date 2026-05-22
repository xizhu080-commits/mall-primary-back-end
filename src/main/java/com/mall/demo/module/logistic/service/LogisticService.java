package com.mall.demo.module.logistic.service;

import com.mall.demo.module.logistic.dto.req.CreateLogisticReqDto;

public interface LogisticService {


    /*
    * 物流通知: 卖家发货        ---->   通知用户
    * */
    void createLogistic(CreateLogisticReqDto createLogisticReqDto);



    /**
     * 物流通知:   物流到达      ---->   通知用户
     */
    void applySignLogistic(String suborderId, String logisticId);

    /**
     * 处理签收/拒签逻辑
     */
    void handleSignLogistic(String logisticId, boolean isAgree, String rejectReason);
}
