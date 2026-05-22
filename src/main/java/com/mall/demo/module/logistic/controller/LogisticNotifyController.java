package com.mall.demo.module.logistic.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.logistic.entity.LogisticNotifyMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logistic/notify")
@RequiredArgsConstructor
public class LogisticNotifyController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 获取当前用户的物流通知列表 (从 Redis 获取最近的通知)
     * @param type 类型："USER" 或 "MERCHANT"
     */
    @GetMapping("/list")
    public RestResp<List<LogisticNotifyMessage>> getNotifyList(@RequestParam String type) {
        String userId = SecurityUtils.getId();
        String key = String.format("notify:%s:%s:list", type.toLowerCase(), userId);

        // 从 Redis 获取 List 结构数据
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);

        if (rawList == null) return RestResp.ok(null);

        // 反序列化为对象列表
        List<LogisticNotifyMessage> messages = rawList.stream()
                .map(obj -> objectMapper.convertValue(obj, LogisticNotifyMessage.class))
                .collect(Collectors.toList());

        return RestResp.ok(messages);
    }

    /**
     * 清除通知列表（清空红点）
     */
    @DeleteMapping("/clear")
    public RestResp<Void> clearNotify(@RequestParam String type) {
        String userId = SecurityUtils.getId();
        String key = String.format("notify:%s:%s:list", type.toLowerCase(), userId);
        redisTemplate.delete(key);
        return RestResp.ok(null);
    }
}