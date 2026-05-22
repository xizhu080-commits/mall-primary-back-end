package com.mall.demo.security.filter;

import com.mall.demo.common.util.JwtUtils;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.security.service.UserDetailServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailServiceImpl userDetailService;


//    获取token,并验证token
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
    String token = request.getHeader("Authorization");

    if (token != null && token.startsWith("Bearer ")) {
        token = token.substring(7);
        try {
            if (jwtUtils.validateToken(token)) {
                String id = jwtUtils.getIdFromToken(token).toString();
                // 获取ID（String）
                String type = jwtUtils.getUserTypeFromToken(token);
                // 获取端类型

                if (id != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 根据 ID 和 类型加载用户详情（见下一步对 loadUserByUsername 的改进）
                    // 传入 "ID:TYPE" 格式，让 UserDetailsService 知道去哪张表查
                    UserDetails userDetails = userDetailService.loadUserByUsername(id + ":" + type);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(id, null, userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // 存入工具类
                    SecurityUtils.setContext(id, type);
                }
            }
        } catch (Exception e) {
            log.error("身份验证失败", e);
        }
    }

    try {
        filterChain.doFilter(request, response);
    } finally {
        SecurityUtils.clear();
    }
}


}