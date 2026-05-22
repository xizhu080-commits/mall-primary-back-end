package com.mall.demo.module.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.merchant.entity.Merchant;
import com.mall.demo.module.order.entity.Suborder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    //最新订单
    @Select("""
    <script>
        select *
        from suborder
        where merchant_id = #{merchantId} 
          and status in (2,3,4,5,6) 
          and create_time >= DATE_SUB(NOW(), INTERVAL 1 MONTH)
       ORDER BY create_time DESC
    </script>
""")
    public List<Suborder> getMerchantLatestSuborderList(String merchantId);


//全部订单
    @Select("""
        select *
        from suborder
        where merchant_id = #{merchantId} 
          and status in (2,3,4,5,6) 
     
  
""")
    public List<Suborder> getMerchantAllSuborderList(String merchantId);



    //商品数量
    @Select("""
        select count(*)
        from sku
        where shop_id = #{shopId}
"""
)
    public int getMerchantSkuCount(String shopId);


    //店铺数量
    @Select("""
        select count(*)
        from shop
        where merchant_id = #{merchantId}
""")
    public int getMerchantShopCount(String merchantId);
}
