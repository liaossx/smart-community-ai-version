package com.lsx.parking.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lsx.parking.entity.ParkingSpace;
import com.lsx.parking.mapper.ParkingSpaceMapper;
import com.lsx.parking.service.ParkingSpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "parking.cache.preheat", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ParkingRemainCacheWarmTask {

    private final ParkingSpaceService parkingSpaceService;
    private final ParkingSpaceMapper parkingSpaceMapper;

    /**
     * 定时预热车位余量缓存，减少热点查询的首次落库开销。
     */
    @Scheduled(cron = "${parking.cache.preheat.cron:0 */5 * * * ?}")
    public void warmParkingRemainCache() {
        List<Long> communityIds = parkingSpaceMapper.selectObjs(
                Wrappers.<ParkingSpace>query()
                        .select("DISTINCT community_id")
                        .eq("deleted", 0)
                        .isNotNull("community_id")
        ).stream()
                .filter(Objects::nonNull)
                .map(this::toLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        parkingSpaceService.getRemaining(null);
        for (Long communityId : communityIds) {
            parkingSpaceService.getRemaining(communityId);
        }

        log.info("车位余量缓存预热完成，已预热 {} 个社区和全局统计", communityIds.size());
    }

    private Long toLong(Object value) {
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            log.warn("车位余量缓存预热跳过非法 communityId: {}", value);
            return null;
        }
    }
}
