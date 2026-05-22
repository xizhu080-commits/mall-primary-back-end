package com.mall.demo.module.Refund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_record")
public class Refund {

    //退款记录:谁申请退款,退的是什么订单,谁处理的退款


    //主键
    @TableId(type = IdType.ASSIGN_ID)
    private String refundId;

    private String orderId;
    private String suborderId;
    private String userId;
    private String merchantId;
    private String paymentId;
    private BigDecimal refundAmount;
    private String refundReason;
    //'状态:0:退款中 1:退款成功 2:退款失败'
    private Integer status;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}