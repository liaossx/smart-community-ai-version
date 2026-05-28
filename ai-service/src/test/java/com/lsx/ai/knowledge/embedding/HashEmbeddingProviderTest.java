package com.lsx.ai.knowledge.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashEmbeddingProviderTest {

    @Test
    void shouldReturnConfiguredDimension() {
        HashEmbeddingProvider provider = new HashEmbeddingProvider(64);

        EmbeddingVector vector = provider.embed("厨房水槽漏水，水已经流到客厅");

        assertThat(vector.dimension()).isEqualTo(64);
    }

    @Test
    void relatedTextsShouldHaveHigherSimilarityThanUnrelatedTexts() {
        HashEmbeddingProvider provider = new HashEmbeddingProvider(128);

        EmbeddingVector question = provider.embed("厨房漏水怎么报修");
        EmbeddingVector related = provider.embed("居民发现厨房漏水后，应先关闭就近水阀，并在业主端提交报修");
        EmbeddingVector unrelated = provider.embed("访客进入小区前应进行访客登记");

        assertThat(question.cosineSimilarity(related))
                .isGreaterThan(question.cosineSimilarity(unrelated));
    }
}
