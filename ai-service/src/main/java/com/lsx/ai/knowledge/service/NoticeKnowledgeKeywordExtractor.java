package com.lsx.ai.knowledge.service;

import com.lsx.ai.knowledge.model.NoticeKnowledgeRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NoticeKnowledgeKeywordExtractor {
    private static final Pattern BUILDING_PATTERN = Pattern.compile("\\d+栋|\\d+幢|\\d+号楼");
    private static final List<String> DOMAIN_KEYWORDS = List.of(
            "停水", "停电", "供水", "供电", "电梯", "消防", "演练", "维修", "报修",
            "物业费", "缴费", "欠费", "停车", "车位", "访客", "门禁", "装修",
            "噪音", "投诉", "垃圾分类", "水箱", "燃气", "漏水", "漏电", "周一",
            "周二", "周三", "周四", "周五", "周六", "周日", "六一", "儿童节",
            "游园", "活动", "高空抛物", "电动车", "充电", "门禁卡", "养犬",
            "文明养犬", "防蚊", "灭蚊", "消杀", "亲水平台", "安全警示"
    );

    public String extract(NoticeKnowledgeRecord notice) {
        Set<String> keywords = new LinkedHashSet<>();
        addIfText(keywords, notice.getCommunityName());
        addIfText(keywords, notice.getTargetBuilding());
        addBuildingKeywords(keywords, notice.getTitle());
        addBuildingKeywords(keywords, notice.getContent());

        String source = (notice.getTitle() == null ? "" : notice.getTitle())
                + " "
                + (notice.getContent() == null ? "" : notice.getContent());
        for (String keyword : DOMAIN_KEYWORDS) {
            if (source.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        return String.join(",", new ArrayList<>(keywords));
    }

    private void addBuildingKeywords(Set<String> keywords, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        Matcher matcher = BUILDING_PATTERN.matcher(text);
        while (matcher.find()) {
            keywords.add(matcher.group());
        }
    }

    private void addIfText(Set<String> keywords, String value) {
        if (StringUtils.hasText(value)) {
            keywords.add(value.trim());
        }
    }
}
