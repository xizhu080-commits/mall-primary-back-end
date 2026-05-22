package com.mall.demo.module.user.dto.resp;

import lombok.Data;

@Data
public class UserLoginRespDto {

    private String token;

    private String userId;

    private String userName;
    private String phone;
    private Integer role;
}
