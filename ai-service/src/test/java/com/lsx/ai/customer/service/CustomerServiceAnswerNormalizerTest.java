package com.lsx.ai.customer.service;

import com.lsx.ai.customer.dto.CustomerServiceAnswerResponse;
import com.lsx.ai.customer.knowledge.KnowledgeDocument;
import com.lsx.ai.customer.knowledge.RetrievedKnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerServiceAnswerNormalizerTest {

    @Test
    void fillsMetadataSourcesAndValidCitations() {
        CustomerServiceAnswerResponse response = new CustomerServiceAnswerResponse();
        response.setAnswer("请在业主端提交报修。");
        response.setCitations(List.of("BAD_SOURCE"));
        response.setConfidence(120);

        KnowledgeDocument document = new KnowledgeDocument(
                "PROCESS_REPAIR_001",
                "REPAIR_PROCESS",
                "Repair flow",
                "Repair content",
                List.of("repair"),
                null
        );

        CustomerServiceAnswerResponse result = new CustomerServiceAnswerNormalizer()
                .normalize(response,
                        List.of(new RetrievedKnowledgeDocument(document, 30)),
                        "SPRING_AI_RAG",
                        "rag-v1",
                        "deepseek-v4-flash");

        assertThat(result.getProvider()).isEqualTo("SPRING_AI_RAG");
        assertThat(result.getProviderVersion()).isEqualTo("rag-v1");
        assertThat(result.getModel()).isEqualTo("deepseek-v4-flash");
        assertThat(result.getConfidence()).isEqualTo(100);
        assertThat(result.getCannotAnswer()).isFalse();
        assertThat(result.getCitations()).containsExactly("PROCESS_REPAIR_001");
        assertThat(result.getSources()).hasSize(1);
        assertThat(result.getSources().get(0).getSourceId()).isEqualTo("PROCESS_REPAIR_001");
        assertThat(result.getSources().get(0).getKeywordScore()).isEqualTo(30);
        assertThat(result.getSources().get(0).getVectorScore()).isZero();
        assertThat(result.getSources().get(0).getRetrievalMode()).isEqualTo("KEYWORD");
    }

    @Test
    void raisesLowModelConfidenceWhenRetrievalScoreIsStrong() {
        CustomerServiceAnswerResponse response = new CustomerServiceAnswerResponse();
        response.setAnswer("小区将举办六一游园会。");
        response.setConfidence(9);

        KnowledgeDocument document = new KnowledgeDocument(
                "sys_notice:24",
                "COMMUNITY_NOTICE",
                "六一儿童节游园活动预告",
                "为了让孩子们度过快乐的节日，物业将于6月1日下午举办游园会。",
                List.of("六一", "儿童节", "活动"),
                2L
        );

        CustomerServiceAnswerResponse result = new CustomerServiceAnswerNormalizer()
                .normalize(response,
                        List.of(new RetrievedKnowledgeDocument(document, 32)),
                        "SPRING_AI_RAG",
                        "rag-v1",
                        "deepseek-v4-flash");

        assertThat(result.getConfidence()).isEqualTo(90);
        assertThat(result.getCannotAnswer()).isFalse();
    }

    @Test
    void floorsCannotAnswerConfidenceToThirty() {
        CustomerServiceAnswerResponse response = new CustomerServiceAnswerResponse();
        response.setAnswer("暂未找到相关资料。");
        response.setCannotAnswer(true);
        response.setConfidence(0);

        CustomerServiceAnswerResponse result = new CustomerServiceAnswerNormalizer()
                .normalize(response,
                        List.of(),
                        "RAG_RETRIEVAL",
                        "rag-v1",
                        "deepseek-v4-flash");

        assertThat(result.getConfidence()).isEqualTo(30);
        assertThat(result.getCannotAnswer()).isTrue();
    }
}
