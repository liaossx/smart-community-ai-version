package com.lsx.core.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        // 配置 ObjectMapper 以支持 LocalDateTime 等 Java 8 时间类型
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL); // 过时
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        om.registerModule(new JavaTimeModule());

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(om);

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(30));

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("adminStatsOverview", baseConfig.entryTtl(Duration.ofSeconds(120)));
        configs.put("noticeUserList", baseConfig.entryTtl(Duration.ofSeconds(30)));
        configs.put("noticeUnreadCount", baseConfig.entryTtl(Duration.ofSeconds(10)));
        
        // 核心业务缓存
        configs.put("houseInfo", baseConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("communityInfo", baseConfig.entryTtl(Duration.ofMinutes(10)));
        configs.put("userDetail", baseConfig.entryTtl(Duration.ofMinutes(2)));
        configs.put("userList", baseConfig.entryTtl(Duration.ofMinutes(1)));
        configs.put("parkingRemain", baseConfig.entryTtl(Duration.ofSeconds(10)));
        configs.put("mySpaces", baseConfig.entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(configs)
                .transactionAware()
                .build();
    }
}
