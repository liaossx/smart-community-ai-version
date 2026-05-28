package com.lsx.system.client.fallback;

import com.lsx.system.client.UserServiceClient;
import com.lsx.system.dto.external.UserInfoDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {
    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {
            @Override
            public UserInfoDTO getUserById(Long id) {
                return null;
            }

            @Override
            public Long countUsers() {
                return 0L;
            }

            @Override
            public Long countUsersByRole(String role) {
                return 0L;
            }
        };
    }
}
