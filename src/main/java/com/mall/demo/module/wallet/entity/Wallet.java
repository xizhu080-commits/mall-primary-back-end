package com.mall.demo.module.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Wallet {

    // 主键(用户/商家)
    @TableId(type = IdType.INPUT)  // INPUT 表示手动输入，不自动生成
    private String id;
    // 0:用户 1:商家
    private Integer type;

    // 余额
    private BigDecimal balance;

    //银行卡
    private String bankCard;
    private String bankName;


}
