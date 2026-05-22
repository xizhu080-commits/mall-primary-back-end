package com.mall.demo.security.service;

import com.mall.demo.module.merchant.entity.Merchant;
import com.mall.demo.module.merchant.service.MerchantService;
import com.mall.demo.module.user.entity.User;
import com.mall.demo.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {


  //  private final AdminService adminService;
    private final UserService userService;
    private final MerchantService merchantService;

    @Override
    public UserDetails loadUserByUsername(String combinedKey) {
        // 解析出 ID 和 类型
        String[] parts = combinedKey.split(":");
        String id = parts[0];
        String type = parts[1];

        String password = "";
        String role = "";

        switch (type) {
            case "MERCHANT":
                Merchant merchant = merchantService.getMerchantByMerchantId(id);
                // 商家表
                if (merchant == null) throw new UsernameNotFoundException("商家不存在");
                password = merchant.getPassword();
                role = "ROLE_MERCHANT";
                break;
         /*   case "ADMIN":
                Admin admin = adminService.getById(id);
                // 管理员表
                if (admin == null) throw new UsernameNotFoundException("管理员不存在");
                password = admin.getPassword();
                role = "ROLE_ADMIN";
                break;*/
            case "USER":
            default:
                User user = userService.getUserByUserId(id);
                // 用户表
                if (user == null) throw new UsernameNotFoundException("用户不存在");
                password = user.getPassword();
                role = "ROLE_USER";
                break;
        }

        return new org.springframework.security.core.userdetails.User(
                id,
                password,
                List.of(new SimpleGrantedAuthority(role))
        );
    }




}