package com.lsx.ai.knowledge.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@ConditionalOnProperty(prefix = "smart-community.ai.embedding", name = "provider", havingValue = "hash", matchIfMissing = true)
public class HashEmbeddingProvider implements EmbeddingProvider {
    private static final int DEFAULT_DIMENSION = 128;
    private static final int MIN_DIMENSION = 32;
    private static final int MAX_DIMENSION = 1024;

    private final int dimension;

    public HashEmbeddingProvider(@Value("${smart-community.ai.embedding.hash.dimension:128}") Integer dimension) {
        this.dimension = safeDimension(dimension);
    }

    @Override
    public EmbeddingVector embed(String text) {
        double[] vector = new double[dimension];
        for (String token : tokens(text)) {
            addFeature(vector, token, 1.0d);
            addFeature(vector, "#" + token.substring(0, 1), 0.35d);
        }
        return new EmbeddingVector(vector).normalized();
    }

    @Override
    public String provider() {
        return "HASH";
    }

    @Override
    public String model() {
        return "hash-ngram-v1";
    }

    @Override
    public int dimension() {
        return dimension;
    }

    private void addFeature(double[] vector, String feature, double weight) {
        int hash = murmurLikeHash(feature);
        int index = Math.floorMod(hash, dimension);
        int sign = (hash & 1) == 0 ? 1 : -1;
        vector[index] += sign * weight;
    }

    private List<String> tokens(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        String[] parts = normalized.split("[,.;:!?，。；：！？、\\s]+");
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            tokens.add(part);
            addCharacterNgrams(tokens, part, 2);
            addCharacterNgrams(tokens, part, 3);
        }
        return tokens;
    }

    private void addCharacterNgrams(List<String> tokens, String text, int n) {
        if (text.length() < n) {
            return;
        }
        for (int i = 0; i + n <= text.length(); i++) {
            tokens.add(text.substring(i, i + n));
        }
    }

    private int murmurLikeHash(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int hash = 0x9747b28c;
        for (byte b : bytes) {
            hash ^= b;
            hash *= 0x5bd1e995;
            hash ^= hash >>> 15;
        }
        return hash;
    }

    private int safeDimension(Integer dimension) {
        if (dimension == null) {
            return DEFAULT_DIMENSION;
        }
        if (dimension < MIN_DIMENSION) {
            return MIN_DIMENSION;
        }
        return Math.min(dimension, MAX_DIMENSION);
    }
}
