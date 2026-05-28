package com.lsx.ai.customer.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HybridCommunityKnowledgeRetrieverTest {

    @Test
    void mergesKeywordAndVectorScoresForSameChunk() {
        KnowledgeDocument document = new KnowledgeDocument(
                "sys_notice:28",
                "COMMUNITY_NOTICE",
                "关于规范电动车充电的通知",
                "小区内已增设集中充电桩，请勿进楼入户充电。",
                List.of("电动车", "充电"),
                2L
        );

        List<RetrievedKnowledgeDocument> results = HybridCommunityKnowledgeRetriever.mergeResults(
                List.of(new RetrievedKnowledgeDocument(document, 18, 18, 0, "KEYWORD")),
                List.of(new RetrievedKnowledgeDocument(document, 22, 0, 64, "VECTOR")),
                3
        );

        assertThat(results).hasSize(1);
        RetrievedKnowledgeDocument merged = results.get(0);
        assertThat(merged.getRetrievalMode()).isEqualTo("HYBRID");
        assertThat(merged.getKeywordScore()).isEqualTo(18);
        assertThat(merged.getVectorScore()).isEqualTo(64);
        assertThat(merged.getScore()).isGreaterThan(22);
    }

    @Test
    void keepsVectorOnlyResultsWhenKeywordRetrieverMisses() {
        KnowledgeDocument document = new KnowledgeDocument(
                "FAQ_REPAIR_LEAK_001",
                "FAQ",
                "厨房漏水如何报修",
                "先关闭就近水阀，再提交报修。",
                List.of("漏水"),
                null
        );

        List<RetrievedKnowledgeDocument> results = HybridCommunityKnowledgeRetriever.mergeResults(
                List.of(),
                List.of(new RetrievedKnowledgeDocument(document, 19, 0, 55, "VECTOR")),
                3
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRetrievalMode()).isEqualTo("VECTOR");
        assertThat(results.get(0).getVectorScore()).isEqualTo(55);
    }

    @Test
    void dropsWeakVectorOnlyResultsWhenThereIsNoKeywordAnchor() {
        KnowledgeDocument noisy1 = new KnowledgeDocument(
                "sys_notice:20",
                "COMMUNITY_NOTICE",
                "夏季防蚊灭蚊专项行动",
                "物业将于每周五下午对全区进行药物喷洒。",
                List.of("蚊虫"),
                2L
        );
        KnowledgeDocument noisy2 = new KnowledgeDocument(
                "POLICY_VISITOR_001",
                "PROPERTY_POLICY",
                "访客登记制度",
                "访客进入小区前应进行访客登记。",
                List.of("访客"),
                null
        );

        List<RetrievedKnowledgeDocument> results = HybridCommunityKnowledgeRetriever.mergeResults(
                List.of(),
                List.of(
                        new RetrievedKnowledgeDocument(noisy1, 14, 0, 41, "VECTOR"),
                        new RetrievedKnowledgeDocument(noisy2, 11, 0, 31, "VECTOR")
                ),
                3
        );

        assertThat(results).isEmpty();
    }

    @Test
    void suppressesWeakVectorOnlyTailWhenThereIsStrongHybridAnchor() {
        KnowledgeDocument anchor = new KnowledgeDocument(
                "sys_notice:28",
                "COMMUNITY_NOTICE",
                "关于规范电动车充电的通知",
                "小区内已增设集中充电桩，请勿进楼入户充电。",
                List.of("电动车", "充电"),
                2L
        );
        KnowledgeDocument noisy1 = new KnowledgeDocument(
                "sys_notice:20",
                "COMMUNITY_NOTICE",
                "夏季防蚊灭蚊专项行动",
                "物业将于每周五下午对全区进行药物喷洒。",
                List.of("蚊虫"),
                2L
        );
        KnowledgeDocument noisy2 = new KnowledgeDocument(
                "sys_notice:27",
                "COMMUNITY_NOTICE",
                "旧版门禁卡升级公告",
                "小区将全面升级人脸识别系统。",
                List.of("门禁卡"),
                2L
        );

        List<RetrievedKnowledgeDocument> results = HybridCommunityKnowledgeRetriever.mergeResults(
                List.of(new RetrievedKnowledgeDocument(anchor, 34, 34, 0, "KEYWORD")),
                List.of(
                        new RetrievedKnowledgeDocument(anchor, 19, 0, 53, "VECTOR"),
                        new RetrievedKnowledgeDocument(noisy1, 15, 0, 43, "VECTOR"),
                        new RetrievedKnowledgeDocument(noisy2, 15, 0, 42, "VECTOR")
                ),
                3
        );

        assertThat(results)
                .extracting(result -> result.getDocument().getSourceId())
                .containsExactly("sys_notice:28");
    }

    @Test
    void suppressesBorderlineVectorOnlyTailWhenHybridAnchorIsClearlyBetter() {
        KnowledgeDocument anchor = new KnowledgeDocument(
                "sys_notice:28",
                "COMMUNITY_NOTICE",
                "关于规范电动车充电的通知",
                "小区内已增设集中充电桩，请勿进楼入户充电。",
                List.of("电动车", "充电"),
                2L
        );
        KnowledgeDocument noisy = new KnowledgeDocument(
                "sys_notice:20",
                "COMMUNITY_NOTICE",
                "夏季防蚊灭蚊专项行动",
                "物业将于每周五下午对全区进行药物喷洒。",
                List.of("蚊虫"),
                2L
        );

        List<RetrievedKnowledgeDocument> results = HybridCommunityKnowledgeRetriever.mergeResults(
                List.of(new RetrievedKnowledgeDocument(anchor, 36, 36, 0, "KEYWORD")),
                List.of(
                        new RetrievedKnowledgeDocument(anchor, 20, 0, 56, "VECTOR"),
                        new RetrievedKnowledgeDocument(noisy, 17, 0, 49, "VECTOR")
                ),
                3
        );

        assertThat(results)
                .extracting(result -> result.getDocument().getSourceId())
                .containsExactly("sys_notice:28");
    }
}
