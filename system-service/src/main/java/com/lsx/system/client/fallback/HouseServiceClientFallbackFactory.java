package com.lsx.system.client.fallback;

import com.lsx.system.client.HouseServiceClient;
import com.lsx.system.dto.external.HouseDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class HouseServiceClientFallbackFactory implements FallbackFactory<HouseServiceClient> {
    @Override
    public HouseServiceClient create(Throwable cause) {
        return new HouseServiceClient() {
            @Override
            public HouseDTO getHouseById(Long id) {
                return null;
            }

            @Override
            public List<HouseDTO> getHouseListByIds(List<Long> ids) {
                return Collections.emptyList();
            }

            @Override
            public List<Long> searchHouseIds(String keyword) {
                return Collections.emptyList();
            }

            @Override
            public Long countCommunities() {
                return 0L;
            }

            @Override
            public Long countOwnersByCommunityId(Long communityId) {
                return 0L;
            }

            @Override
            public String getCommunityNameById(Long id) {
                return null;
            }
        };
    }
}
