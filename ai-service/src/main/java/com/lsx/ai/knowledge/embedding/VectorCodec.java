package com.lsx.ai.knowledge.embedding;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;

@Component
public class VectorCodec {

    public String encode(EmbeddingVector vector) {
        double[] values = vector == null ? new double[0] : vector.getValues();
        StringBuilder builder = new StringBuilder(values.length * 10);
        builder.append("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(String.format(Locale.ROOT, "%.6f", values[i]));
        }
        builder.append("]");
        return builder.toString();
    }

    public EmbeddingVector decode(String text) {
        if (!StringUtils.hasText(text)) {
            return new EmbeddingVector(new double[0]);
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (!StringUtils.hasText(cleaned)) {
            return new EmbeddingVector(new double[0]);
        }
        double[] values = Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .mapToDouble(Double::parseDouble)
                .toArray();
        return new EmbeddingVector(values);
    }
}
