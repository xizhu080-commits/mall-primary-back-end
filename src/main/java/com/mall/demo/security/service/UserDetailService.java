package com.mall.demo.security.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserDetailService {


    UserDetails loadUserByUserIdOrPhone(String phoneOrUserId);
}
