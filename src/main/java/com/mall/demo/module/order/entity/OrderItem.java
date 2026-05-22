package com.mall.demo.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_item") // 对应数据库表名
public class OrderItem {
    @TableId(type = IdType.ASSIGN_ID)
    private String itemId;

    private String userId;
    private String orderId;
    private String skuId;
    private String productName;
    private String productUrl;


    private Integer quantity;
    private BigDecimal price;
}
