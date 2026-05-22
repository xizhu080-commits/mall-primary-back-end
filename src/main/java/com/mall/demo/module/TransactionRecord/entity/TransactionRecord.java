package com.mall.demo.module.TransactionRecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transaction_record")
public class TransactionRecord {
    // 主键
    @TableId(type = IdType.ASSIGN_ID)
    private String transactionId;
    // 用户编号
    private String userId;
    // 交易金额
    private BigDecimal payAmount;
    // 交易时间
    private LocalDateTime payTime;
    // 交易状态: 1->交易成功；2->退款成功
    private Integer transactionStatus;
    // 订单编号
    private String orderId;
    // 子订单编号
    private String suborderId;

   //支付类型
    private String payType;

    //商品名字内容
    private String productNameText;

    //备注
    private String remark;


    private LocalDateTime updateTime;

}