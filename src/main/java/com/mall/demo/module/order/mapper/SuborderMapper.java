package com.mall.demo.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.order.dto.resp.GetSuborderDetailRespDto;
import com.mall.demo.module.order.dto.resp.SuborderItemRespDto;
import com.mall.demo.module.order.entity.Suborder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SuborderMapper extends BaseMapper<Suborder> {

    @Select(
            """
            select * 
            from suborder 
            where suborder_id = #{suborderId}
            FOR UPDATE
"""
    )
    Suborder selectByIdForUpdate(String suborderId);


    @Select(
            """
            select *
            from suborder 
            where user_id = #{userId}
         
"""
    )
    List<Suborder> getSuborderListByUserId(String userId);


    @Select(
            """
            select * 
            from suborder 
            where order_id = #{orderId}
"""
    )
    List<Suborder> getSuborderListByIdOrderId(String orderId);





    /*
    * 获取子订单数量
    * */
    @Select(
            """
        select count(*)
        from suborder
        where user_id = #{userId}
        Or
        merchant_id = #{merchantId}
"""
    )
    Integer getSuborderCountById(String id);



    /*
     * 获取消息数量
     * */
    @Select("""
        SELECT (
            SELECT COUNT(*) FROM logistic_notify_message 
            WHERE target_user_id = #{userId} OR target_user_id = #{merchantId}
        ) + (
            SELECT COUNT(*) FROM refund_notify_message 
            WHERE target_user_id = #{userId} OR target_user_id = #{merchantId}
        ) + (
            SELECT COUNT(*) FROM payment_notify_message 
            WHERE target_user_id = #{userId} OR target_user_id = #{merchantId}
        ) AS total_count
""")
    Integer getMessageCountById(String id);


    /*
    * 获取优惠卷数量
    * */
    @Select(
            """
            select count(*)
            from coupon_user
            where user_id = #{userId}
             
"""
    )
    Integer getCouponCountById(String id);

































    @Select("""
            SELECT 
                s.suborder_id AS suborderId,
                s.order_id AS orderId,
                s.merchant_id AS merchantId,
                s.shop_id AS shopId,
                s.shop_name AS shopName,
                sh.shop_logo AS shopLogo,
                s.sku_id AS skuId,
                sp.spu_id AS spuId,
                s.product_name AS productName,
                s.spec_data AS specData,
                s.product_url AS productUrl,
                s.price,
                s.discount_amount AS discountAmount,
                s.pay_amount AS payAmount,
                s.status,
                s.remark,
                s.receiver_name AS receiverName,
                s.receiver_phone AS receiverPhone,
                s.address,
                
                -- 物流信息（LEFT JOIN，未发货时取子订单数据）
                l.logistic_id AS logisticId,
                l.logistic_company_name AS logisticCompanyName,
                l.consignee AS logisticConsignee,
                l.consignee_address AS logisticAddress,
                l.sign_time AS signTime,
                
                -- 支付信息
                p.pay_type AS payType,
                p.payment_id AS paymentId,
                p.pay_time AS payTime,
                p.create_time AS paymentCreateTime,
                s.create_time AS suborderCreateTime
                
            FROM suborder s
            LEFT JOIN logistic l ON s.order_id = l.order_id 
                                AND s.suborder_id = l.suborder_id 
                                AND l.buyer_id = #{userId}
            LEFT JOIN payment p ON s.order_id = p.order_id 
                               AND p.user_id = #{userId}
            LEFT JOIN sku sp ON s.sku_id = sp.sku_id
            LEFT JOIN shop sh ON s.shop_id = sh.shop_id
            
            WHERE s.user_id = #{userId}
            ORDER BY s.create_time DESC
            """)
    List<GetSuborderDetailRespDto> getSuborderDetailListByUserId(String userId);



}
