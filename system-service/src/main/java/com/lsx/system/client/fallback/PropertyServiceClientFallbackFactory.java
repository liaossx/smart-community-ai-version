package com.lsx.system.client.fallback;

import com.lsx.core.common.Result.Result;
import com.lsx.system.client.PropertyServiceClient;
import com.lsx.system.dto.NoticeCreateDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PropertyServiceClientFallbackFactory implements FallbackFactory<PropertyServiceClient> {
    @Override
    public PropertyServiceClient create(Throwable cause) {
        return new PropertyServiceClient() {
            @Override
            public Result<Long> createNoticeInner(NoticeCreateDTO dto, Long userId, String internalToken) {
                return Result.fail("调用物业服务失败: " + cause.getMessage());
            }

            @Override
            public Map<String, Object> getComplaintStats() {
                Map<String, Object> m = new HashMap<>();
                m.put("total", 0);
                m.put("pending", 0);
                m.put("processed", 0);
                return m;
            }

            @Override
            public List<Map<String, Object>> getComplaintTypeStats() {
                return Collections.emptyList();
            }

            @Override
            public Map<String, Object> getRepairStats() {
                Map<String, Object> m = new HashMap<>();
                m.put("total", 0);
                m.put("pending", 0);
                m.put("today", 0);
                m.put("processing", 0);
                m.put("cancelled", 0);
                m.put("completed", 0);
                return m;
            }

            @Override
            public List<Map<String, Object>> getRepairTrend() {
                return Collections.emptyList();
            }

            @Override
            public Map<String, Object> getVisitorStats() {
                Map<String, Object> m = new HashMap<>();
                m.put("total", 0);
                m.put("approved", 0);
                m.put("rejected", 0);
                m.put("pending", 0);
                return m;
            }
        };
    }
}
