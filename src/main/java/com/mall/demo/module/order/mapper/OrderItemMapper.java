package com.mall.demo.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select(
"""
 SELECT * 
FROM order_item 
WHERE order_no = #{orderId}
""")
    List<OrderItem> selectByOrderId(@Param("orderId") String orderId);




}
