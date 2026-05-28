package com.lsx.ai.knowledge.service;

import com.lsx.ai.knowledge.model.NoticeKnowledgeRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeKnowledgeKeywordExtractorDomainTest {

    @Test
    void extractsCommonNoticeDomainKeywords() {
        NoticeKnowledgeRecord notice = new NoticeKnowledgeRecord();
        notice.setTitle("六一儿童节游园活动预告");
        notice.setContent("小区将开展亲子游园活动。");

        String keywords = new NoticeKnowledgeKeywordExtractor().extract(notice);

        assertThat(keywords).contains("六一", "儿童节", "游园", "活动");
    }
}
