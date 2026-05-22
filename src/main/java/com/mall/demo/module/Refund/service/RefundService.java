package com.mall.demo.module.Refund.service;

import com.mall.demo.module.Refund.dto.req.HandleRefundReqDto;
import com.mall.demo.module.Refund.dto.req.ApplyRefundReqDto;
import com.mall.demo.module.Refund.dto.resp.ApplyRefundRespDto;
import com.mall.demo.module.Refund.dto.resp.HandleRefundRespDto;

public interface RefundService {


    /**
     * 用户申请退款         -------    通知商家
     */
    ApplyRefundRespDto applyRefund(ApplyRefundReqDto dto);


    /*
    * 商家处理退款         -------    通知用户
    * */
    HandleRefundRespDto handleRefund(HandleRefundReqDto dto);

}
