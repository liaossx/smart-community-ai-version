package com.lsx.ai.knowledge.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "smart-community.ai.embedding", name = "provider", havingValue = "openai")
public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {
    private static final int DEFAULT_DIMENSION = 1536;

    private final EmbeddingModel embeddingModel;
    private final String model;
    private volatile int dimension;

    public OpenAiCompatibleEmbeddingProvider(ObjectProvider<EmbeddingModel> embeddingModelProvider,
                                             @Value("${smart-community.ai.embedding.openai.model:text-embedding-3-small}")
                                             String model,
                                             @Value("${smart-community.ai.embedding.openai.dimension:1536}")
                                             Integer configuredDimension) {
        this.embeddingModel = embeddingModelProvider.getIfAvailable();
        if (this.embeddingModel == null) {
            throw new IllegalStateException(
                    "No Spring AI EmbeddingModel bean available. Configure spring.ai.openai.embedding.* " +
                            "or switch AI_EMBEDDING_PROVIDER=hash");
        }
        this.model = StringUtils.hasText(model) ? model : "text-embedding-3-small";
        this.dimension = safeDimension(configuredDimension);
    }

    @Override
    public EmbeddingVector embed(String text) {
        float[] values = embeddingModel.embed(text);
        double[] vector = new double[values == null ? 0 : values.length];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = values[i];
        }
        if (vector.length > 0) {
            this.dimension = vector.length;
        }
        return new EmbeddingVector(vector).normalized();
    }

    @Override
    public String provider() {
        return "OPENAI_COMPATIBLE";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    private int safeDimension(Integer configuredDimension) {
        if (configuredDimension == null || configuredDimension <= 0) {
            return DEFAULT_DIMENSION;
        }
        return configuredDimension;
    }
}
