package com.mall.demo.module.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.shop.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {

    /*
    * 获取商家的所有店铺
    * */
    @Select(
            """
                    select shop_id, merchant_id, shop_name, shop_logo
                    from shop 
                    where merchant_id = #{merchantId}
                    
                    """
    )
    public List<Shop> getShopListByMerchantId(String merchantId);




}
