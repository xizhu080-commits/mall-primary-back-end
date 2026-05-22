package com.mall.demo.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mall.demo.module.logistic.entity.Address;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private String userId;

    private String userName;


    private String password;


    private String userAvatar = "https://storage.360buyimg.com/default.image/6a645f6465665f696d675f393836323131373632333134353935323236_sma.jpg";



    private String phone;


    private String email = null;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer sex = 0;

    /**
     * 角色状态：0-普通用户，1-作者/商家，2-管理员
     */
    private Integer role = 0;

    /**
     * 账号状态：1-正常，0-封禁
     */
    private Integer status = 1;




    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic // 逻辑删除
    private Integer isDeleted;
}