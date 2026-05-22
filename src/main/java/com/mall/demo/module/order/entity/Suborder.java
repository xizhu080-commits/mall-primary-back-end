package com.mall.demo.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("suborder")
public class Suborder {
    //主键
    @TableId(type = IdType.ASSIGN_ID)
    private String suborderId;
    //买家
    private String userId;
    private String userName;
    //收货人
    private String receiverName;
    private String receiverPhone;
    private String merchantId;
    private String shopName;
    private String shopId;
    private String skuId;
    private String specData;
    private String orderId;
    private BigDecimal price;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String productName;
    private String productUrl;
    private Integer payType;

    /**
     * 订单状态: 1->待付款；2->待发货；3->已发货；4->已完成；5->退款中; 6->退款成功;  0->已关闭
     */
    private Integer status;

    private Integer isDeleted;
    private String address;

    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;


    //物流公司
    private String logisticCompanyName;

}
