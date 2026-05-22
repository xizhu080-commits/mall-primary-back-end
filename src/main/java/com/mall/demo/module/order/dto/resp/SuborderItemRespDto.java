package com.mall.demo.module.order.dto.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 子订单列表项响应 DTO
 */
@Data
public class SuborderItemRespDto {

    /**
     * 子订单ID
     */
    private String suborderId;

    /**
     * 主订单ID
     */
    private String orderId;

    /**
     * 商品ID
     */
    private String skuId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片
     */
    private String productUrl;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 状态
     */
    private Integer status;





}