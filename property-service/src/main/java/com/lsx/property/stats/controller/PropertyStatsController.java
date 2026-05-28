package com.lsx.property.stats.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lsx.core.common.Util.UserContext;
import com.lsx.property.complaint.entity.SysComplaint;
import com.lsx.property.complaint.service.ComplaintService;
import com.lsx.property.visitor.entity.SysVisitor;
import com.lsx.property.visitor.service.VisitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PropertyStatsController {

    @Resource
    private ComplaintService complaintService;

    @Resource
    private VisitorService visitorService;

    // 投诉统计
    @GetMapping({"/complaint/stats", "/complaint/admin/stats"})
    public Map<String, Object> getComplaintStats() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        Map<String, Object> complaintStats = new HashMap<>();
        LambdaQueryWrapper<SysComplaint> complaintWrapper = new LambdaQueryWrapper<>();
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            complaintWrapper.eq(SysComplaint::getCommunityId, communityId);
        }
        // 总数
        complaintStats.put("total", complaintService.count(complaintWrapper));
        // 待处理
        complaintStats.put("pending", complaintService.count(complaintWrapper.clone().eq(SysComplaint::getStatus, "PENDING")));
        // 已处理 (非PENDING)
        complaintStats.put("processed", complaintService.count(complaintWrapper.clone().ne(SysComplaint::getStatus, "PENDING")));
        return complaintStats;
    }

    @GetMapping({"/complaint/stats/type", "/complaint/admin/stats/type"})
    public List<Map<String, Object>> getComplaintTypeStats() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        QueryWrapper<SysComplaint> typeWrapper = new QueryWrapper<>();
        typeWrapper.select("type as name, count(*) as value").groupBy("type");
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            typeWrapper.eq("community_id", communityId);
        }
        return complaintService.listMaps(typeWrapper);
    }

    // 访客统计
    @GetMapping({"/visitor/stats", "/visitor/admin/stats"})
    public Map<String, Object> getVisitorStats() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        Map<String, Object> visitorStats = new HashMap<>();
        LambdaQueryWrapper<SysVisitor> visitorWrapper = new LambdaQueryWrapper<>();
        if (!"super_admin".equalsIgnoreCase(role) && communityId != null) {
            visitorWrapper.eq(SysVisitor::getCommunityId, communityId);
        }
        visitorStats.put("total", visitorService.count(visitorWrapper));
        visitorStats.put("pending", visitorService.count(visitorWrapper.clone().eq(SysVisitor::getStatus, "PENDING")));
        visitorStats.put("approved", visitorService.count(visitorWrapper.clone().eq(SysVisitor::getStatus, "APPROVED")));
        visitorStats.put("rejected", visitorService.count(visitorWrapper.clone().eq(SysVisitor::getStatus, "REJECTED")));
        return visitorStats;
    }
}
