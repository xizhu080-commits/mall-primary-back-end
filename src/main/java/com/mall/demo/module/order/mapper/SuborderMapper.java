package com.mall.demo.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
