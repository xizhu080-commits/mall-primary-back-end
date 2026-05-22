package com.mall.demo.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.order.entity.MyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<MyOrder> {


        @Update("""
        UPDATE t_order
        SET status = 2
        WHERE order_id = #{orderId}
        AND status = 0
    """)

        int closeOrderIfUnpaid(@Param("orderId") String orderId );
    }



