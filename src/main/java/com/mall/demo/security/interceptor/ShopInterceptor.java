package com.mall.demo.security.interceptor;

import com.mall.demo.common.util.SecurityUtils;  // 导入您的 SecurityUtils
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ShopInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头提取 shopId
        String shopId = request.getHeader("Shop-Id");

        if (StringUtils.isNotEmpty(shopId)) {
            // 存入 SecurityUtils 中的 ThreadLocal
            SecurityUtils.setCurrentShopId(shopId);
        }

        return true;
        // 放行请求
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理（注意：这里只清理 shopId）
        // 如果 clear() 会清理所有，需要确认其他地方是否还在使用
        SecurityUtils.setCurrentShopId(null);
        // 或者调用完整的 clear()
    }
}