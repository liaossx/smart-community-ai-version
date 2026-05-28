package com.lsx.ai.customer.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KeywordCommunityKnowledgeRetriever {
    /*
     * RAG 的“检索阶段”。
     *
     * 现在使用关键词打分来模拟检索能力：问题命中资料关键词越多，分数越高。
     * 真正接向量数据库时，通常会把这个类替换成 VectorStore 检索器。
     */
    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 5;
    private static final int MIN_TOP_SCORE_FOR_RELATIVE_FILTER = 10;
    private static final double MIN_RELATIVE_SCORE_RATIO = 0.30d;
    private static final int MIN_FRAGMENT_LENGTH = 2;
    private static final int MAX_FRAGMENT_LENGTH = 6;
    private static final Set<String> STOP_FRAGMENTS = Set.of(
            "小区", "社区", "物业", "居民", "业主", "什么", "怎么", "如何", "需要", "可以",
            "有没有", "是不是", "是否", "一下", "注意", "通知", "公告"
    );

    private final CommunityKnowledgeRepository repository;

    public KeywordCommunityKnowledgeRetriever(CommunityKnowledgeRepository repository) {
        this.repository = repository;
    }

    public List<RetrievedKnowledgeDocument> retrieve(String question, Long communityId, Integer topK) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        int limit = safeTopK(topK);
        List<RetrievedKnowledgeDocument> scoredResults = repository.findAll().stream()
                // 社区专属资料只允许对应社区命中，通用制度 communityId 为 null。
                .filter(document -> matchesCommunity(document, communityId))
                // 给每份资料计算相关性分数。
                .map(document -> new RetrievedKnowledgeDocument(document, score(document, question)))
                .filter(result -> result.getScore() > 0)
                // 分数越高越靠前，再按相对分数过滤掉明显不相关的资料。
                .sorted(Comparator.comparingInt(RetrievedKnowledgeDocument::getScore).reversed())
                .collect(Collectors.toList());
        return filterLowRelevance(scoredResults).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<RetrievedKnowledgeDocument> filterLowRelevance(List<RetrievedKnowledgeDocument> results) {
        if (results.isEmpty()) {
            return results;
        }
        int bestScore = results.get(0).getScore();
        if (bestScore < MIN_TOP_SCORE_FOR_RELATIVE_FILTER) {
            return results;
        }
        int minAllowedScore = (int) Math.ceil(bestScore * MIN_RELATIVE_SCORE_RATIO);
        return results.stream()
                .filter(result -> result.getScore() >= minAllowedScore)
                .collect(Collectors.toList());
    }

    private boolean matchesCommunity(KnowledgeDocument document, Long communityId) {
        return document.getCommunityId() == null
                || communityId == null
                || document.getCommunityId().equals(communityId);
    }

    private int score(KnowledgeDocument document, String question) {
        String normalizedQuestion = normalize(question);
        int score = 0;
        // 关键词命中是主评分逻辑，例如“停水”“3栋”“周六”。
        for (String keyword : document.getKeywords()) {
            String normalizedKeyword = normalize(keyword);
            if (normalizedQuestion.contains(normalizedKeyword)) {
                score += 10 + normalizedKeyword.length();
            }
        }
        // 再用标题和正文做一层弱匹配，避免关键词漏配时完全搜不到。
        String searchable = normalize(document.getTitle() + " " + document.getContent());
        for (String token : splitQuestion(normalizedQuestion)) {
            if (token.length() >= 2 && searchable.contains(token)) {
                score += Math.min(token.length(), 8);
            }
            score += scoreTextFragments(searchable, token);
        }
        return score;
    }

    private int scoreTextFragments(String searchable, String token) {
        int score = 0;
        for (String fragment : textFragments(token)) {
            if (!STOP_FRAGMENTS.contains(fragment) && searchable.contains(fragment)) {
                score += Math.min(fragment.length(), 6);
            }
        }
        return score;
    }

    private Set<String> textFragments(String token) {
        Set<String> fragments = new LinkedHashSet<>();
        if (!StringUtils.hasText(token) || token.length() < MIN_FRAGMENT_LENGTH) {
            return fragments;
        }
        int maxLength = Math.min(MAX_FRAGMENT_LENGTH, token.length());
        for (int length = maxLength; length >= MIN_FRAGMENT_LENGTH; length--) {
            for (int start = 0; start + length <= token.length(); start++) {
                fragments.add(token.substring(start, start + length));
            }
        }
        return fragments;
    }

    private List<String> splitQuestion(String question) {
        return List.of(question.split("[,.;:!?，。；：！？、\\s]+"));
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private int safeTopK(Integer topK) {
        // 限制 topK，防止一次塞太多资料进 Prompt，导致上下文过长。
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }
}
