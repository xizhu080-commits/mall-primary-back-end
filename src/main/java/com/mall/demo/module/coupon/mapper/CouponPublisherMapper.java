package com.mall.demo.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.coupon.entity.CouponPublisher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CouponPublisherMapper extends BaseMapper<CouponPublisher> {


    //根据shopId获取优惠卷模板ID
    @Select("""
        select template_id
        from coupon_publisher
        where shop_id = #{shopId}

""")
    public List<String> getTemplateIdsByShopId(String shopId);

}
