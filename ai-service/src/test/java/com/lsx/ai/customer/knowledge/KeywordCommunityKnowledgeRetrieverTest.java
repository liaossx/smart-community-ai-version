package com.lsx.ai.customer.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordCommunityKnowledgeRetrieverTest {

    @Test
    void retrievesRepairKnowledgeForLeakQuestion() {
        KeywordCommunityKnowledgeRetriever retriever =
                new KeywordCommunityKnowledgeRetriever(new StaticCommunityKnowledgeRepository());

        List<RetrievedKnowledgeDocument> results = retriever.retrieve("厨房水管漏水怎么报修", 1L, 3);

        assertThat(results).isNotEmpty();
        assertThat(results)
                .extracting(result -> result.getDocument().getSourceId())
                .contains("PROCESS_REPAIR_001", "POLICY_REPAIR_001");
    }

    @Test
    void filtersCommunitySpecificNotice() {
        KeywordCommunityKnowledgeRetriever retriever =
                new KeywordCommunityKnowledgeRetriever(new StaticCommunityKnowledgeRepository());

        List<RetrievedKnowledgeDocument> otherCommunityResults = retriever.retrieve("3栋周六是不是停水", 2L, 3);

        assertThat(otherCommunityResults)
                .extracting(result -> result.getDocument().getSourceId())
                .doesNotContain("NOTICE_WATER_001");
    }

    @Test
    void retrievesNoticeByChineseTitleFragmentsWhenKeywordsAreSparse() {
        CommunityKnowledgeRepository repository = () -> List.of(new KnowledgeDocument(
                "sys_notice:24",
                "COMMUNITY_NOTICE",
                "六一儿童节游园活动预告",
                "本周六下午在中心花园开展亲子游园活动。",
                List.of(),
                2L
        ));
        KeywordCommunityKnowledgeRetriever retriever = new KeywordCommunityKnowledgeRetriever(repository);

        List<RetrievedKnowledgeDocument> results = retriever.retrieve("六一儿童节小区有什么活动", 2L, 3);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocument().getSourceId()).isEqualTo("sys_notice:24");
    }

    @Test
    void filtersLowRelevanceDocumentsWhenTopResultIsStrong() {
        CommunityKnowledgeRepository repository = () -> List.of(
                new KnowledgeDocument(
                        "sys_notice:7",
                        "COMMUNITY_NOTICE",
                        "关于严禁高空抛物的警示",
                        "近期发现个别楼层有高空抛物现象，这是违法行为。",
                        List.of("高空抛物"),
                        1L
                ),
                new KnowledgeDocument(
                        "POLICY_FEE_001",
                        "PROPERTY_POLICY",
                        "物业费缴费规定",
                        "物业费可通过业主端线上缴纳。",
                        List.of(),
                        null
                )
        );
        KeywordCommunityKnowledgeRetriever retriever = new KeywordCommunityKnowledgeRetriever(repository);

        List<RetrievedKnowledgeDocument> results = retriever.retrieve("高空抛物有什么规定？", 1L, 3);

        assertThat(results)
                .extracting(result -> result.getDocument().getSourceId())
                .containsExactly("sys_notice:7");
    }
}
