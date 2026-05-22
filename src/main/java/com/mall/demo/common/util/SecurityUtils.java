package com.mall.demo.common.util;



/*public class SecurityUtils {

    private static final ThreadLocal<Long> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_THREAD_LOCAL.set(userId);
    }

    public static Long getUserId() {
        return USER_THREAD_LOCAL.get();
    }

    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}*/

public class SecurityUtils {
    private static final ThreadLocal<String> ID_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<String> TYPE_THREAD_LOCAL = new ThreadLocal<>();

    public static void setContext(String id, String type) {
        ID_THREAD_LOCAL.set(id);
        TYPE_THREAD_LOCAL.set(type);
    }

    public static String getId() { return ID_THREAD_LOCAL.get(); }
    public static String getType() { return TYPE_THREAD_LOCAL.get(); }




    private static final ThreadLocal<String> CURRENT_SHOP = new ThreadLocal<>();

    public static void setCurrentShopId(String shopId) {
        CURRENT_SHOP.set(shopId);
    }

    public static String getCurrentShopId() {
        return CURRENT_SHOP.get();
    }
    public static void clear() {
        ID_THREAD_LOCAL.remove();
        TYPE_THREAD_LOCAL.remove();
        CURRENT_SHOP.remove();
    }

}