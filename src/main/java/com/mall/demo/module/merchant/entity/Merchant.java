package com.mall.demo.module.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "商家唯一标识ID")
    private String merchantId;




    @Schema(description = "商家密码")
    private String password;

    @Schema(description = "商家名称")
    private String merchantName;

    private String merchantAvatar;

    @Schema(description = "主营品牌名称")
    private String brand;


    @Schema(description = "商家真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "商家证件号")
    private String idCard;


    @Schema(description = "商家营业执照")
    private String qualificationCertificate;

    @Schema(description = "商家简介")
    private String intro;

    @Schema(description = "商家状态：1-正常，0-禁用")
    private Integer merchantStatus;
    // 显式命名状态

    private LocalDateTime createTime;
    private LocalDateTime updateTime;


    private Integer role;

}
