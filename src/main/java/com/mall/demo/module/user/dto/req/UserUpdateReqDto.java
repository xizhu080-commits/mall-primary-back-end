package com.mall.demo.module.user.dto.req;

import com.mall.demo.module.logistic.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新用户dto")
public class UserUpdateReqDto {


    private String newPhone;


    @Schema(description = "新用户名")
    private String newUserName;

    @Schema(description = "新密码")
    private String newPassword;


    @Schema(description = "新头像")
    private String newUserAvatar;





    @Schema(description = "新性别")
    private Integer newSex;





}
