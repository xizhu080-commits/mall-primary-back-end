package com.mall.demo.module.wallet.dto.req;

import lombok.Data;
@Data
public class AddBankCardReqDto {

    // 银行卡号
    private String bankCard;

    // 银行名称
    private String bankName;


}
