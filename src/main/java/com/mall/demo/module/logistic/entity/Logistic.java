package com.mall.demo.module.logistic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistic")
public class Logistic {
    @TableId(type = IdType.ASSIGN_ID)
    private String logisticId;
    //物流公司(101:顺丰速运 102:圆通速运 103:中通速运 104:韵达速运  105:邮政速运 106:申通速运)
    private String logisticCompanyId;
    private String logisticCompanyName;


    //买家id
    private String buyerId;

    private String orderId;
    private String suborderId;

    //发货人(卖家)
    private String shipper;
    private String shipperId;
    private String shipperAddress;
    private LocalDateTime shipperTime;

    //派送时间
    private LocalDateTime deliveryTime;
    //签收时间(即送达时间)
    private LocalDateTime signTime;

    //自动签收时间
    private LocalDateTime autoSignTime;

    //收货人
    private String consignee;
    private String consigneeId;
    private String consigneeAddress;
    //收货时间
    private LocalDateTime consigneeTime;


    //状态:0->派送中；1->待签收；2->已签收；3->已拒签；4->退回中；5->已退回
    private Integer status;


    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}