package com.mall.demo.module.coupon.mapper;

import com.alipay.api.domain.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.coupon.entity.CouponUsedRecord;
import com.mall.demo.module.coupon.entity.CouponUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponUserMapper extends BaseMapper<CouponUser> {


    // 如果只需要未使用且未过期的优惠券
    @Select(
            """
                    SELECT coupon_id,  template_id, status, 
                           start_time, end_time, used_time, expire_time, 
                           create_time, update_time
                    FROM coupon_user 
                    WHERE user_id = #{userId} 
                      AND status = 0 
                      AND end_time > NOW()
                    """)
    public List<CouponUser> getAllCoupon(String userId);

    // CouponUserMapper.java
    @Update("UPDATE coupon_user " +
            "SET status = 1, used_time = NOW(), update_time = NOW() " +
            "WHERE user_id = #{userId} AND status = 0 AND coupon_id = #{couponId}")
    int markAsUsed(@Param("userId") String userId,
                   @Param("couponId") String couponId);

}
