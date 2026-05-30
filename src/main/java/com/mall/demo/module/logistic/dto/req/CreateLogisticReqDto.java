package com.mall.demo.module.logistic.dto.req;

import lombok.Data;

@Data
public class CreateLogisticReqDto {

    //发货人
    private String shipper;
    private String shipperId;
    private String shipperAddress;


    //收货人
    private String consignee;
    private String consigneeId;
    private String consigneePhone;
    private String consigneeAddress;

    private String orderId;
    private String suborderId;


    //物流公司(SF101:顺丰速运 YT102:圆通速运 ZT103:中通速运 YT104:韵达速运  YZ105:邮政速运 ST106:申通速运)
    private String logisticCompanyId;
    private String logisticCompanyName;
}
