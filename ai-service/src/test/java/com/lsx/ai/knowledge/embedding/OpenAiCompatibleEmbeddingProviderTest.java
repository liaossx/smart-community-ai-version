package com.lsx.ai.knowledge.embedding;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Iterator;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleEmbeddingProviderTest {

    @Test
    void shouldUseConfiguredModelAndNormalizeReturnedVector() {
        EmbeddingModel embeddingModel = new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public float[] embed(Document document) {
                throw new UnsupportedOperationException();
            }

            @Override
            public float[] embed(String text) {
                return new float[]{3.0f, 4.0f};
            }

            @Override
            public int dimensions() {
                return 2;
            }
        };

        OpenAiCompatibleEmbeddingProvider provider = new OpenAiCompatibleEmbeddingProvider(
                fixedProvider(embeddingModel),
                "text-embedding-3-small",
                1536
        );

        EmbeddingVector vector = provider.embed("电动车在哪里充电");

        assertThat(provider.provider()).isEqualTo("OPENAI_COMPATIBLE");
        assertThat(provider.model()).isEqualTo("text-embedding-3-small");
        assertThat(provider.dimension()).isEqualTo(2);
        assertThat(vector.dimension()).isEqualTo(2);
        assertThat(vector.getValues()[0]).isCloseTo(0.6d, org.assertj.core.data.Offset.offset(0.0001d));
        assertThat(vector.getValues()[1]).isCloseTo(0.8d, org.assertj.core.data.Offset.offset(0.0001d));
    }

    private ObjectProvider<EmbeddingModel> fixedProvider(EmbeddingModel embeddingModel) {
        return new ObjectProvider<>() {
            @Override
            public EmbeddingModel getObject(Object... args) {
                return embeddingModel;
            }

            @Override
            public EmbeddingModel getIfAvailable() {
                return embeddingModel;
            }

            @Override
            public EmbeddingModel getIfUnique() {
                return embeddingModel;
            }

            @Override
            public EmbeddingModel getObject() {
                return embeddingModel;
            }

            @Override
            public void forEach(Consumer<? super EmbeddingModel> action) {
                action.accept(embeddingModel);
            }

            @Override
            public Iterator<EmbeddingModel> iterator() {
                return java.util.List.of(embeddingModel).iterator();
            }
        };
    }
}
