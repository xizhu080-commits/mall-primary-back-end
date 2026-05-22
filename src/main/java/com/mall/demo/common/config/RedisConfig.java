package com.mall.demo.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 企业级 Redis 配置类
 * - 提供 RedisTemplate
 * - 配置 JSON 序列化
 * - 适用于 Java 21 + Spring Boot 3.x
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}") // 密码可选
    private String redisPassword;

    @Value("${spring.data.redis.database:0}") // 默认数据库0
    private int redisDatabase;

    /**
     * Redis 连接工厂
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (!redisPassword.isBlank()) {
            config.setPassword(redisPassword);
        }
        config.setDatabase(redisDatabase);
        return new LettuceConnectionFactory(config);
    }

    /**
     * RedisTemplate 配置 - 用于对象存储
     * - key 使用 String
     * - value 使用 JSON 序列化
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 使用字符串序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // value 使用 JSON 序列化
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate 配置 - 用于字符串存储
     * - key 和 value 都使用 String 序列化
     * - 专门用于验证码等字符串场景
     */
    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 和 value 都使用字符串序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * StringRedisTemplate - Spring Boot 默认提供的便捷模板
     * - key 和 value 都是 String 类型
     * - 用于简单的字符串操作
     */
    @Bean
    public StringRedisTemplate stringRedisTemplateForConvenience(LettuceConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate(factory);

        // 设置序列化器（可选，StringRedisTemplate 默认就是 String 序列化）
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        return template;
    }

        @Bean
        public DefaultRedisScript<Long> stockLuaScript() {

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();

            script.setScriptText(
                    "local stock = tonumber(redis.call('GET', KEYS[1])) " +
                            "if stock == nil then return -1 end " +
                            "if stock < tonumber(ARGV[1]) then return -2 end " +
                            "redis.call('DECRBY', KEYS[1], ARGV[1]) " +
                            "return 1"
            );

            script.setResultType(Long.class);

            return script;
        }

}