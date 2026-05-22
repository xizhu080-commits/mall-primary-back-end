package com.mall.demo.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("coupon_publisher")
public class CouponPublisher {

    @TableId
    private String templateId;
    // 优惠券模板ID（主键）

    @TableField("publisher_id")
    private String publisherId;
    // 发布者ID
    private String shopId;

    @TableField("publisher_type")
    private Integer publisherType;
    // 发布类型：1平台(店铺) 2商家

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    // 发布时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    // 修改时间
}