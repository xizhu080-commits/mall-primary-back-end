package com.mall.demo.module.Refund.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.demo.common.result.RestResp;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.Refund.entity.RefundNotifyMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/refund/notify")
@RequiredArgsConstructor
public class RefundNotifyController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 获取退款通知列表
     * @param type 身份类型：MERCHANT(商家) 或 USER(用户)
     */
    @GetMapping("/list")
    public RestResp<List<RefundNotifyMessage>> list(@RequestParam String type) {
        String userId = SecurityUtils.getId();
        // 修正之前提到的 %d 占位符问题，这里统一使用 %s
        String key = String.format("notify:%s:%s:list", type.toLowerCase(), userId);

        // 获取 Redis 中的最近通知（List结构）
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);

        if (rawList == null || rawList.isEmpty()) {
            return RestResp.ok();
        }

        // 将 JSON 字符串转换回对象
        List<RefundNotifyMessage> messages = rawList.stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj.toString(), RefundNotifyMessage.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .collect(Collectors.toList());

        return RestResp.ok(messages);
    }

    /**
     * 清除通知列表（消除红点）
     */
    @DeleteMapping("/clear")
    public RestResp<Void> clear(@RequestParam String type) {
        String userId = SecurityUtils.getId();
        String key = String.format("notify:%s:%s:list", type.toLowerCase(), userId);
        redisTemplate.delete(key);
        return RestResp.ok("通知已清空", null);
    }
}