package com.mall.demo.module.Refund.dto.req;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandleRefundReqDto {

    //商家处理结果

        //退款记录id
    private String refundId;
    //处理结果
    private Boolean isAgree;
    private String suborderId;
    private String orderId;


    //拒绝退款理由
    private String rejectReason;
}
