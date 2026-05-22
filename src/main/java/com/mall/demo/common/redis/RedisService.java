package com.mall.demo.common.redis;

import cn.hutool.core.util.StrUtil;
import com.mall.demo.common.result.PageResp;
import com.mall.demo.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    private static final String EMPTY_VALUE = "null";
    private static final long EMPTY_TTL = 2; // 空值缓存（分钟）
    private static final int RANDOM_TTL_BOUND = 5; // 防雪崩随机值（分钟）

    /**
     * ==============================
     * ✅ 通用 set（防雪崩）
     * ==============================
     */
    public void set(String key, String value, long ttlMinutes) {
        long random = new Random().nextInt(RANDOM_TTL_BOUND);
        redisTemplate.opsForValue().set(
                key,
                value,
                Duration.ofMinutes(ttlMinutes + random)
        );
    }

    /**
     * ==============================
     * ✅ 删除缓存
     * ==============================
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }






    /**
     * ==============================
     * ✅ 1. 单对象缓存（详情页）
     * 👉 防击穿 + 防穿透 + 防雪崩
     * ==============================
     */
    public <T> T get(
            String key,
            Class<T> clazz,
            Function<String, T> dbCallback,
            //Function<T,R>  T传参，R返回值  作用：把T转换成R，缓存中存储的是R
            // dbCallback：Function<String, PageResp<T>>的名字
            long ttlMinutes
    ) {

        // 1️⃣ 查缓存
        String json = redisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(json)) {
            if (EMPTY_VALUE.equals(json)) {
                return null;
            }
            return JsonUtils.toObj(json, clazz);
        }

        // 2️⃣ 防击穿：加分布式锁
        RLock lock = redissonClient.getLock("lock:" + key);

        try {
            lock.lock();

            // 🔁 双检
            json = redisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)) {
                if (EMPTY_VALUE.equals(json)) {
                    return null;
                }
                return JsonUtils.toObj(json, clazz);
            }

            // 3️⃣ 查数据库
            T data = dbCallback.apply(key);

            // 4️⃣ 防穿透
            if (data == null) {
                redisTemplate.opsForValue().set(
                        key,
                        EMPTY_VALUE,
                        Duration.ofMinutes(EMPTY_TTL)
                );
                return null;
            }

            // 5️⃣ 写缓存（防雪崩）
            this.set(key, JsonUtils.toJson(data), ttlMinutes);

            return data;

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * ==============================
     * ✅ 2. 简单列表缓存（首页推荐）
     * 👉 不加锁（高并发）
     * ==============================
     */
    public <T> List<T> getList(
            String key,
            Class<T> clazz,
            Function<String, List<T>> dbCallback,
            //Function<T,R>  T传参，R返回值  作用：把T转换成R，缓存中存储的是R
            // dbCallback：Function<String, PageResp<T>>的名字
            long ttlMinutes
    ) {

        // 1️⃣ 查缓存
        String json = redisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(json)) {
            if (EMPTY_VALUE.equals(json)) {
                return Collections.emptyList();
            }
            return JsonUtils.toList(json, clazz);
        }

        // 2️⃣ 查数据库
        List<T> list = dbCallback.apply(key);

        // 3️⃣ 防穿透
        if (list == null || list.isEmpty()) {
            redisTemplate.opsForValue().set(
                    key,
                    EMPTY_VALUE,
                    Duration.ofMinutes(EMPTY_TTL)
            );
            return Collections.emptyList();
        }

        // 4️⃣ 写缓存
        this.set(key, JsonUtils.toJson(list), ttlMinutes);

        return list;
    }

    /**
     * ==============================
     * ✅ 3. 分页缓存（搜索模块）
     * 👉 PageRest
     * ==============================
     */
    public <T> PageResp<T> getPage(
            String key,
            Class<T> clazz,
            Function<String, PageResp<T>> dbCallback,
            //Function<T,R>  T传参，R返回值  作用：把T转换成R，缓存中存储的是R
            // dbCallback：Function<String, PageResp<T>>的名字
            long ttlMinutes
    ) {

        // 1️⃣ 查缓存
        String json = redisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(json)) {
            if (EMPTY_VALUE.equals(json)) {
                return emptyPage();
            }
            return JsonUtils.toPage(json, clazz);
        }

        // 🚨 不加锁（避免锁竞争）

        // 2️⃣ 查数据库
        PageResp<T> pageData = dbCallback.apply(key);

        // 3️⃣ 防穿透
        if (pageData == null || pageData.getList().isEmpty()) {
            redisTemplate.opsForValue().set(
                    key,
                    EMPTY_VALUE,
                    Duration.ofMinutes(EMPTY_TTL)
            );
            return emptyPage();
        }

        // 4️⃣ 写缓存
        this.set(key, JsonUtils.toJson(pageData), ttlMinutes);

        return pageData;
    }

    /**
     * ==============================
     * ✅ 空分页对象
     * ==============================
     */
    private <T> PageResp<T> emptyPage() {
        return PageResp.of(0L, 0L, 0L, Collections.emptyList());
    }
}