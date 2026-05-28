package com.lsx.ai.customer.knowledge;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Primary
public class HybridCommunityKnowledgeRetriever {
    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 5;
    private static final int HYBRID_MATCH_BONUS = 5;
    private static final int MIN_VECTOR_ONLY_SCORE_WITH_KEYWORD_ANCHOR = 50;
    private static final double MIN_VECTOR_ONLY_RATIO_WITH_KEYWORD_ANCHOR = 0.95d;
    private static final int MIN_VECTOR_ONLY_ACCEPT_SCORE_WITHOUT_KEYWORD = 50;

    private final KeywordCommunityKnowledgeRetriever keywordRetriever;
    private final VectorCommunityKnowledgeRetriever vectorRetriever;

    public HybridCommunityKnowledgeRetriever(KeywordCommunityKnowledgeRetriever keywordRetriever,
                                             ObjectProvider<VectorCommunityKnowledgeRetriever> vectorRetrieverProvider) {
        this.keywordRetriever = keywordRetriever;
        this.vectorRetriever = vectorRetrieverProvider.getIfAvailable();
    }

    public List<RetrievedKnowledgeDocument> retrieve(String question, Long communityId, Integer topK) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        int limit = safeTopK(topK);
        List<RetrievedKnowledgeDocument> keywordResults = keywordRetriever.retrieve(question, communityId, limit);
        List<RetrievedKnowledgeDocument> vectorResults = vectorRetriever == null
                ? List.of()
                : vectorRetriever.retrieve(question, communityId, limit);
        return mergeResults(keywordResults, vectorResults, limit);
    }

    static List<RetrievedKnowledgeDocument> mergeResults(List<RetrievedKnowledgeDocument> keywordResults,
                                                         List<RetrievedKnowledgeDocument> vectorResults,
                                                         int limit) {
        Map<String, MergeCandidate> merged = new LinkedHashMap<>();
        for (RetrievedKnowledgeDocument result : safeList(keywordResults)) {
            merged.computeIfAbsent(documentKey(result.getDocument()), key -> new MergeCandidate(result.getDocument()))
                    .acceptKeyword(result);
        }
        for (RetrievedKnowledgeDocument result : safeList(vectorResults)) {
            merged.computeIfAbsent(documentKey(result.getDocument()), key -> new MergeCandidate(result.getDocument()))
                    .acceptVector(result);
        }
        List<RetrievedKnowledgeDocument> results = merged.values().stream()
                .map(MergeCandidate::toResult)
                .filter(result -> result.getScore() > 0)
                .sorted(Comparator.comparingInt(RetrievedKnowledgeDocument::getScore).reversed()
                        .thenComparing(Comparator.comparingInt(RetrievedKnowledgeDocument::getKeywordScore).reversed())
                        .thenComparing(Comparator.comparingInt(RetrievedKnowledgeDocument::getVectorScore).reversed()))
                .collect(Collectors.toList());
        return suppressWeakVectorOnlyTail(results).stream()
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    private static List<RetrievedKnowledgeDocument> safeList(List<RetrievedKnowledgeDocument> results) {
        return results == null ? List.of() : results;
    }

    private static String documentKey(KnowledgeDocument document) {
        return String.join("|",
                document.getSourceId(),
                Objects.toString(document.getTitle(), ""),
                Integer.toHexString(Objects.hashCode(document.getContent())));
    }

    private int safeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private static List<RetrievedKnowledgeDocument> suppressWeakVectorOnlyTail(List<RetrievedKnowledgeDocument> results) {
        int bestAnchoredVectorScore = results.stream()
                .filter(result -> result.getKeywordScore() > 0)
                .mapToInt(RetrievedKnowledgeDocument::getVectorScore)
                .max()
                .orElse(0);
        if (bestAnchoredVectorScore <= 0) {
            int bestVectorOnlyScore = results.stream()
                    .mapToInt(RetrievedKnowledgeDocument::getVectorScore)
                    .max()
                    .orElse(0);
            if (bestVectorOnlyScore < MIN_VECTOR_ONLY_ACCEPT_SCORE_WITHOUT_KEYWORD) {
                return List.of();
            }
            int minVectorOnlyScore = Math.max(
                    MIN_VECTOR_ONLY_ACCEPT_SCORE_WITHOUT_KEYWORD,
                    (int) Math.ceil(bestVectorOnlyScore * 0.90d)
            );
            return results.stream()
                    .filter(result -> result.getVectorScore() >= minVectorOnlyScore)
                    .collect(Collectors.toList());
        }
        int minVectorOnlyScore = Math.max(
                MIN_VECTOR_ONLY_SCORE_WITH_KEYWORD_ANCHOR,
                (int) Math.ceil(bestAnchoredVectorScore * MIN_VECTOR_ONLY_RATIO_WITH_KEYWORD_ANCHOR)
        );
        List<RetrievedKnowledgeDocument> filtered = results.stream()
                .filter(result -> result.getKeywordScore() > 0 || result.getVectorScore() >= minVectorOnlyScore)
                .collect(Collectors.toList());
        return filtered.isEmpty() ? results : filtered;
    }

    private static final class MergeCandidate {
        private final KnowledgeDocument document;
        private int keywordScore;
        private int vectorScore;

        private MergeCandidate(KnowledgeDocument document) {
            this.document = document;
        }

        private void acceptKeyword(RetrievedKnowledgeDocument result) {
            this.keywordScore = Math.max(this.keywordScore, result.getKeywordScore());
        }

        private void acceptVector(RetrievedKnowledgeDocument result) {
            this.vectorScore = Math.max(this.vectorScore, result.getVectorScore());
        }

        private RetrievedKnowledgeDocument toResult() {
            int finalScore = mergedScore(keywordScore, vectorScore);
            return new RetrievedKnowledgeDocument(
                    document,
                    finalScore,
                    keywordScore,
                    vectorScore,
                    retrievalMode(keywordScore, vectorScore)
            );
        }

        private int mergedScore(int keywordScore, int vectorScore) {
            int vectorContribution = Math.round(vectorScore * 0.35f);
            int baseScore = Math.max(keywordScore, vectorContribution);
            if (keywordScore > 0 && vectorScore > 0) {
                baseScore += HYBRID_MATCH_BONUS;
            }
            return baseScore;
        }

        private String retrievalMode(int keywordScore, int vectorScore) {
            if (keywordScore > 0 && vectorScore > 0) {
                return "HYBRID";
            }
            if (keywordScore > 0) {
                return "KEYWORD";
            }
            return "VECTOR";
        }
    }
}
