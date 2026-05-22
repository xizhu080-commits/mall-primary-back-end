package com.mall.demo.module.logistic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("address")
public class Address {

    @TableId(type = IdType.ASSIGN_ID)
    private String addressId;

    private String userId;

    private String addressName;

    private String phone;

    private String receiver;

}
