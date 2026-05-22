package com.mall.demo.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coupon_used_record")
public class CouponUsedRecord {


    private String couponId;
    private String orderId;
    private String userId;
    private Integer status;
    private LocalDateTime usedTime;

}
