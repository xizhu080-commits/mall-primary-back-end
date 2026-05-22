package com.mall.demo.module.logistic.dto.req;

import lombok.Data;

@Data
public class UpdateAddressReqDto
{

    private String addressId;
    private String addressName;
    private String phone;
    private String receiver;

}
