package com.lsx.house.service;

import com.lsx.core.common.Result.Result;
import com.lsx.house.client.UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class HouseBindAsyncService {
    @Resource
    private UserServiceClient userServiceClient;

    @Async
    public void syncUserCommunityIdIfEmpty(Long userId, Long communityId) {
        for (int i = 0; i < 3; i++) {
            try {
                Result<Boolean> r = userServiceClient.updateUserCommunityIdIfEmpty(userId, communityId);
                if (r != null && r.getCode() != null && r.getCode() == 200) {
                    return;
                }
                String msg = r != null ? r.getMsg() : "null response";
                log.warn("syncUserCommunityIdIfEmpty failed userId={} communityId={} attempt={} msg={}", userId, communityId, i + 1, msg);
            } catch (Exception e) {
                log.warn("syncUserCommunityIdIfEmpty exception userId={} communityId={} attempt={}", userId, communityId, i + 1, e);
            }
            try {
                Thread.sleep(200L * (i + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
