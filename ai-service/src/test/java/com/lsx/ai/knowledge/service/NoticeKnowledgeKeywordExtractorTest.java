package com.lsx.ai.knowledge.service;

import com.lsx.ai.knowledge.model.NoticeKnowledgeRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeKnowledgeKeywordExtractorTest {

    @Test
    void extractsDomainAndBuildingKeywords() {
        NoticeKnowledgeRecord notice = new NoticeKnowledgeRecord();
        notice.setCommunityName("阳光花园");
        notice.setTitle("二次供水水箱清洗停水通知");
        notice.setContent("3栋和4栋本周六09:00至12:00暂停供水，请居民提前储水。");

        String keywords = new NoticeKnowledgeKeywordExtractor().extract(notice);

        assertThat(keywords).contains("阳光花园", "3栋", "4栋", "停水", "供水", "水箱", "周六");
    }
}
