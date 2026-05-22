package com.mall.demo.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Select;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

        //根据templateId获取优惠券模板
    @Select(
            """
            select *
            from coupon_template
            where template_id = #{templateId}
            and status = 0
            and remain_count >= 1
            and end_time > now()
"""
    )
    public CouponTemplate getAvailableTemplateByTemplateId(String templateId);


}
