package com.mall.demo.module.merchant.dto.resp;

import com.mall.demo.module.Refund.entity.RefundNotifyMessage;
import com.mall.demo.module.coupon.entity.CouponPublisher;
import com.mall.demo.module.coupon.entity.CouponTemplate;
import com.mall.demo.module.logistic.entity.LogisticNotifyMessage;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.payment.entity.PaymentNotifyMessage;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.product.entity.SKU;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商家信息响应DTO")
public class GetMerchantInfoRespDto {


    @Schema(description = "商家ID")
    private String merchantId;

    @Schema(description = "真实姓名（展示时建议后端脱敏，如：张*）")
    private String realName;

    @Schema(description = "身份证号（展示时高度脱敏，如：420***********1234）")
    private String idCard;
private String brand;

    @Schema(description = "商家名称")
    private String merchantName;
    @Schema(description = "商家头像")
    private String merchantAvatar;
    @Schema(description = "营业执照")
    private String qualificationCertificate;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "商家简介")
    private String intro;

    @Schema(description = "商家状态：1-正常，0-注销/禁用")
    private Integer merchantStatus;

    @Schema(description = "入驻时间")
    private LocalDateTime createTime;

    //子订单数量
    private Integer suborderCount;
    //消息数量
    private Integer messageCount;
    //店铺数量
    private Integer shopCount;
    //商品数量
    private Integer skuCount;





    @Schema(description = "用户角色：1-普通用户，2-作者，3-管理员")
    private Integer role;

    //最新订单
    private List<Suborder> latestSuborderList;

    //全部订单
    private List<Suborder> allSuborderList;

    //我的店铺
    private List<Shop> myShopList;

    //我的商品
    private List<SKU> mySkuList;

    //我的优惠卷---发布者
    private List<CouponPublisher> myCouponList;

    //我的优惠卷----模板
    private List<CouponTemplate> myCouponTemplateList;

    //我的消息:退款消息
    private List<RefundNotifyMessage> myRefundMessageList;
    //我的消息:物流消息
    private List<LogisticNotifyMessage> myLogisticMessageList;
    //我的消息:支付消息
    private List<PaymentNotifyMessage> myPayMessageList;

}
