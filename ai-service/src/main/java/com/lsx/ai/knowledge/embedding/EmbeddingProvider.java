package com.lsx.ai.knowledge.embedding;

public interface EmbeddingProvider {
    EmbeddingVector embed(String text);

    String provider();

    String model();

    int dimension();
}
