package com.mall.demo.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {


    @Select("""
            select * 
            from payment 
            where  order_id = #{orderId}
            and user_id = #{userId}
            FOR UPDATE
            """)
       Payment getPaymentByIdForUpdate( String orderId, String userId);

}
