package com.mall.demo.module.order.dto.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GetSuborderDetailRespDto {


    /**
     * 子订单ID
     */
    private String suborderId;

    /**
     * 主订单ID
     */
    private String orderId;

    //商家id
    private String merchantId;

    /*
    * 店铺
    * */
    private String shopId;
    private String shopName;
    private String shopLogo;
    /**
     * 商品ID
     */
    private String skuId;
    private String spuId;
    /**
     * 商品名称
     */
    private String productName;

    //商品规格
    private String specData;

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




    /*
     * 备注
     * */
    private String remark;


    //====================================================物流信息===============================================================

    /*
    * 物流单号
    * */
    private String logisticId;

    /*
    * 物流公司
    * */
    private String logisticCompanyName;


    /*
     * 收货人姓名
     * */
    private String receiverName;



    /*
     * 收货地址
     * */
    private String address;

    /*
    * 签收时间
    * */
    private LocalDateTime signTime;


    //==================================================支付信息===============================================================

    /*
     * 支付方式
     */
    private String payType;

    /*
    * 支付单号
    * */
    private String paymentId;


    /*
     * 支付时间
     * */
    private LocalDateTime payTime;

    /*
    * 下单时间
    * */
    private LocalDateTime createTime;





}
