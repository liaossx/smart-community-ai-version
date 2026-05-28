package com.lsx.system.client.fallback;

import com.lsx.system.client.WorkorderServiceClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkorderServiceClientFallbackFactory implements FallbackFactory<WorkorderServiceClient> {
    @Override
    public WorkorderServiceClient create(Throwable cause) {
        return new WorkorderServiceClient() {
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
        };
    }
}
