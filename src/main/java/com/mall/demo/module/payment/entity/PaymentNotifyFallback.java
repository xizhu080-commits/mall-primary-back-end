package com.mall.demo.module.payment.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
@Data
@TableName("payment_notify_fallback")
public class PaymentNotifyFallback {

    @TableId(type = IdType.AUTO)
    private long id;

    private String messageJson;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    //0-待重试,1-成功,2-失败
    private Integer status;

    private LocalDateTime createTime;


}
