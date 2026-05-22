package com.mall.demo.module.order.entity;



import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * 采用大厂通用标准：逻辑删除、自动填充、雪花算法ID
 */
@Data
@TableName("my_order") // 对应数据库表名
public class MyOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID (使用雪花算法生成的分布式唯一ID)
     *   * 订单编号 (全局唯一，用于展示给用户)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String orderId;



    /**
     * 用户ID
     */
    private String userId;

    /**
     * 订单总额
     */
    private BigDecimal totalAmount;

    /**
     * 应付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单状态: 1->待付款；2->待发货；3->已发货；4->已完成；5->退款中; 6->退款成功;0->已关闭
     */
    private Integer status;

    /**
     * 支付方式：1->支付宝；2->微信
     */
    private Integer payType;

    /**
     * 逻辑删除标识 (0-未删除，1-已删除)
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /*
    * 过期时间
    * */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime expireTime;


    private String address;
    private String remark;
    private String receiverName;

}