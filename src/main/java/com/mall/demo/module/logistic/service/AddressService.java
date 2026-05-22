package com.mall.demo.module.logistic.service;


import com.mall.demo.module.logistic.dto.req.CreateAddressReqDto;
import com.mall.demo.module.logistic.dto.req.UpdateAddressReqDto;
import com.mall.demo.module.logistic.dto.resp.GetAddressListRespDto;
import com.mall.demo.module.logistic.entity.Address;

import java.util.List;

public interface AddressService {


    GetAddressListRespDto createAddress(CreateAddressReqDto reqDto);


    GetAddressListRespDto updateAddress(UpdateAddressReqDto reqDto);

    List<Address> getAddressList();


    //删除地址
    void deleteAddress(String addressId);

}
