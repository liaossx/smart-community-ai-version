package com.lsx.community.activity.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.community.activity.entity.SysActivity;
import com.lsx.community.activity.mapper.SysActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStockReconciliationTask {

    private final SysActivityMapper activityMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String STOCK_KEY_PREFIX = "activity:stock:";
    private static final String USER_SET_KEY_PREFIX = "activity:users:";

    /**
     * 每 10 分钟对账一次。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void reconcileActivityStock() {
        log.info("【库存对账】开始扫描活动库存...");

        // 1. 查出所有进行中和报名中的活动
        List<SysActivity> activities = activityMapper.selectList(
                Wrappers.<SysActivity>lambdaQuery()
                        .in(SysActivity::getStatus, "ONLINE", "PUBLISHED")
        );

        if (activities.isEmpty()) {
            log.info("【库存对账】无进行中的活动，跳过");
            return;
        }

        int fixedCount = 0;
        for (SysActivity activity : activities) {
            try {
                if (reconcileOneActivity(activity)) {
                    fixedCount++;
                }
            } catch (Exception e) {
                log.error("【库存对账】活动 {} 对账异常", activity.getId(), e);
            }
        }

        log.info("【库存对账】完成。扫描 {} 个活动，修复 {} 个", activities.size(), fixedCount);
    }

    /**
     * 对单个活动进行对账。
     * 以 MySQL 的 signup_count 为准，修复 Redis。
     *
     * @return true 表示做了修复
     */
    private boolean reconcileOneActivity(SysActivity activity) {
        Long activityId = activity.getId();
        String stockKey = STOCK_KEY_PREFIX + activityId;
        String userSetKey = USER_SET_KEY_PREFIX + activityId;

        // 2. 从 MySQL 获取真实数据
        int maxCount = activity.getMaxCount() == null ? 999999 : activity.getMaxCount();
        int mysqlSignupCount = activity.getSignupCount() == null ? 0 : activity.getSignupCount();
        int expectedStock = maxCount - mysqlSignupCount;

        // 3. 从 Redis 获取当前库存
        String redisStockStr = stringRedisTemplate.opsForValue().get(stockKey);

        if (redisStockStr == null) {
            // Redis 里没有库存 key —— 可能是过期了，或者从未初始化
            log.info("【库存对账】活动 {} 库存 key 不存在，从 MySQL 初始化 expectedStock={}",
                    activityId, expectedStock);
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(expectedStock));
            return true;
        }

        int redisStock = Integer.parseInt(redisStockStr);

        // 4. 对比 Redis Set 里的实际人数 和 MySQL 的 signup_count
        Long redisUserCount = stringRedisTemplate.opsForSet().size(userSetKey);
        if (redisUserCount == null) redisUserCount = 0L;

        if (redisStock == expectedStock) {
            // 数据一致，不修复
            return false;
        }

        // 5. 不一致 —— 以 MySQL 为准修复 Redis
        log.warn("【库存对账】活动 {} 数据不一致！Redis库存={}, 期望库存={}, " +
                 "MySQL报名数={}, Redis报名数={}, 将用 MySQL 数据修复",
                activityId, redisStock, expectedStock, mysqlSignupCount, redisUserCount);

        // 修复 Redis 库存
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(expectedStock));

        return true;
    }
}
