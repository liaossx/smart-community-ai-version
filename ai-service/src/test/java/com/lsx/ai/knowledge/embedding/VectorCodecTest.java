package com.lsx.ai.knowledge.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VectorCodecTest {

    @Test
    void shouldEncodeAndDecodeVector() {
        VectorCodec codec = new VectorCodec();
        EmbeddingVector vector = new EmbeddingVector(new double[]{0.25d, -0.5d, 0.75d});

        EmbeddingVector decoded = codec.decode(codec.encode(vector));

        assertThat(decoded.getValues()).containsExactly(0.25d, -0.5d, 0.75d);
    }
}
