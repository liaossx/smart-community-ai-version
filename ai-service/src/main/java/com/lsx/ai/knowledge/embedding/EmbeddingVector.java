package com.lsx.ai.knowledge.embedding;

import java.util.Arrays;

public class EmbeddingVector {
    private final double[] values;

    public EmbeddingVector(double[] values) {
        this.values = values == null ? new double[0] : values;
    }

    public double[] getValues() {
        return values;
    }

    public int dimension() {
        return values.length;
    }

    public double cosineSimilarity(EmbeddingVector other) {
        if (other == null || values.length == 0 || other.values.length == 0 || values.length != other.values.length) {
            return 0.0d;
        }
        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;
        for (int i = 0; i < values.length; i++) {
            dot += values[i] * other.values[i];
            leftNorm += values[i] * values[i];
            rightNorm += other.values[i] * other.values[i];
        }
        if (leftNorm == 0.0d || rightNorm == 0.0d) {
            return 0.0d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public EmbeddingVector normalized() {
        double norm = Math.sqrt(Arrays.stream(values).map(value -> value * value).sum());
        if (norm == 0.0d) {
            return this;
        }
        double[] normalized = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            normalized[i] = values[i] / norm;
        }
        return new EmbeddingVector(normalized);
    }
}
