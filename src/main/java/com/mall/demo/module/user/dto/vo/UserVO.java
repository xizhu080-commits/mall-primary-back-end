package com.mall.demo.module.user.dto.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户视图对象")
public class UserVO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "脱敏后的用户名")
    private String userName;

    @Schema(description = "脱敏后的手机号")
    private String phone;
    // 例如显示为：138****0000



    @Schema(description = "头像地址")
    private String userAvatar;

    @Schema(description = "性别文字")
    private String sexName;
    // 数据库存1，VO里直接变“男”


    @Schema(description = "角色标签:")
    private String roleLabel;
    // 数据库存1，VO里显示“普通用户”



    @Schema(description = "注册时间格式化")
    private String createTimeStr;
    // 2026-04-10 22:00




    @Schema(description = "是否在线")
    private Boolean isOnline;
    // 业务逻辑动态判断
}
