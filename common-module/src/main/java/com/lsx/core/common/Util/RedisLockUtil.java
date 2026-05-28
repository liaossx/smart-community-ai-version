package com.lsx.core.common.Util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
public class RedisLockUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试加锁
     */
    public boolean tryLock(String key, String value, long expireSeconds) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁（防止误删别人的锁）
     */
    public void unlock(String key, String value) {
        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "    return redis.call('del', KEYS[1]) " +
                        "else " +
                        "    return 0 " +
                        "end";

        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key),
                value
        );

        // 可以记录日志
        if (result != null && result == 1) {
            log.debug("释放锁成功，key: {}, value: {}", key, value);
        } else {
            log.debug("释放锁失败或锁已过期，key: {}, value: {}", key, value);
        }
    }

    /**
     * 尝试获取锁，支持重试
     */
    public boolean tryLockWithRetry(String key, String value,
                                    long expireSeconds,
                                    int maxRetry,
                                    long waitMillis) throws InterruptedException {
        for (int i = 0; i < maxRetry; i++) {
            if (tryLock(key, value, expireSeconds)) {
                return true;
            }
            // 等待后重试
            Thread.sleep(waitMillis);
        }
        return false;
    }
}