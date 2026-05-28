package com.lsx.system.admin.service;

import com.lsx.system.client.HouseServiceClient;
import com.lsx.system.client.PropertyServiceClient;
import com.lsx.system.client.UserServiceClient;
import com.lsx.system.client.WorkorderServiceClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminStatsService {

    @Resource
    private PropertyServiceClient propertyServiceClient;

    @Resource
    private UserServiceClient userServiceClient;

    @Resource
    private HouseServiceClient houseServiceClient;

    @Resource
    private WorkorderServiceClient workorderServiceClient;

    @Cacheable(cacheNames = "adminStatsOverview", key = "#role + ':' + ((#role != null && #role.equalsIgnoreCase('super_admin') && #communityIdParam != null) ? #communityIdParam : #tokenCommunityId)")
    public Map<String, Object> getOverviewData(String role, Long tokenCommunityId, Long communityIdParam) {
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> communityStats = new HashMap<>();
        if ("super_admin".equalsIgnoreCase(role)) {
            communityStats.put("total", houseServiceClient.countCommunities());
        } else {
            communityStats.put("total", tokenCommunityId != null ? 1 : 0);
        }
        data.put("community", communityStats);

        Map<String, Object> complaintStats = propertyServiceClient.getComplaintStats();
        Map<String, Object> repairStats = workorderServiceClient.getRepairStats();
        Map<String, Object> visitorStats = propertyServiceClient.getVisitorStats();
        data.put("complaint", complaintStats);
        data.put("repair", repairStats);
        data.put("visitor", visitorStats);

        Map<String, Object> userStats = new HashMap<>();
        if ("super_admin".equalsIgnoreCase(role)) {
            userStats.put("total", userServiceClient.countUsers());
            userStats.put("owner", userServiceClient.countUsersByRole("owner"));
            userStats.put("admin", userServiceClient.countUsersByRole("admin"));
            userStats.put("worker", userServiceClient.countUsersByRole("worker"));
        } else {
            Long ownerCount = 0L;
            if (tokenCommunityId != null) {
                ownerCount = houseServiceClient.countOwnersByCommunityId(tokenCommunityId);
            }
            userStats.put("total", ownerCount);
            userStats.put("owner", ownerCount);
            userStats.put("admin", 0);
        }
        data.put("user", userStats);

        data.put("complaintType", propertyServiceClient.getComplaintTypeStats());
        data.put("repairTrend", workorderServiceClient.getRepairTrend());

        long todoTotal = 0L;
        todoTotal += getNumberAsLong(complaintStats, "pending");
        todoTotal += getNumberAsLong(visitorStats, "pending");
        todoTotal += getNestedNumberAsLong(repairStats, "repair", "pending");
        todoTotal += getNestedNumberAsLong(repairStats, "workorder", "pending");
        data.put("todoTotal", todoTotal);

        long todayRepair = getNumberAsLong(repairStats, "todayRepair");
        data.put("todayRepair", todayRepair);

        return data;
    }

    private long getNumberAsLong(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return 0L;
        }
        Object val = map.get(key);
        if (val == null) {
            return 0L;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return 0L;
    }

    @SuppressWarnings("unchecked")
    private long getNestedNumberAsLong(Map<String, Object> map, String nestedKey, String key) {
        if (map == null || nestedKey == null || key == null) {
            return 0L;
        }
        Object nested = map.get(nestedKey);
        if (!(nested instanceof Map)) {
            return 0L;
        }
        return getNumberAsLong((Map<String, Object>) nested, key);
    }
}
