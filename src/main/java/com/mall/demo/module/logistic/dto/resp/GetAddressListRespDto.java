package com.mall.demo.module.logistic.dto.resp;


import lombok.Data;

@Data
public class GetAddressListRespDto {

    private String userId;
    private String addressName;
    private String addressId;
    private String  phone;
    private String receiver;

}
