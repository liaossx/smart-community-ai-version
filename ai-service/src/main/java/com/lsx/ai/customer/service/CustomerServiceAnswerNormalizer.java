package com.lsx.ai.customer.service;

import com.lsx.ai.customer.dto.CustomerServiceAnswerResponse;
import com.lsx.ai.customer.dto.RagSource;
import com.lsx.ai.customer.knowledge.RetrievedKnowledgeDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CustomerServiceAnswerNormalizer {

    /*
     * RAG 输出校正器。
     *
     * 大模型返回后不能直接相信，这里负责：
     * 1. 补全 sources/provider/model 等后端字段。
     * 2. 校验 citations 只能引用真实检索到的 sourceId。
     * 3. 资料不足或模型空答时给出安全兜底文案。
     */
    public CustomerServiceAnswerResponse normalize(CustomerServiceAnswerResponse response,
                                                   List<RetrievedKnowledgeDocument> retrievedDocuments,
                                                   String provider,
                                                   String providerVersion,
                                                   String model) {
        CustomerServiceAnswerResponse result = response == null
                ? new CustomerServiceAnswerResponse()
                : response;

        List<RagSource> sources = toSources(retrievedDocuments);
        result.setSources(sources);
        result.setFollowUpActions(nonNull(result.getFollowUpActions()));

        boolean hasSources = !sources.isEmpty();
        if (!StringUtils.hasText(result.getAnswer())) {
            result.setAnswer(hasSources
                    ? "已检索到相关社区资料，请联系物业客服进一步确认。"
                    : "暂未在社区知识库中找到可以回答该问题的资料，请联系物业客服人工确认。");
        }
        if (result.getCannotAnswer() == null) {
            result.setCannotAnswer(!hasSources);
        }
        result.setConfidence(normalizeConfidence(result.getConfidence(), retrievedDocuments, result.getCannotAnswer()));
        result.setCitations(normalizeCitations(result.getCitations(), sources, result.getCannotAnswer()));
        result.setProvider(provider);
        result.setProviderVersion(providerVersion);
        result.setModel(model);
        return result;
    }

    private List<RagSource> toSources(List<RetrievedKnowledgeDocument> retrievedDocuments) {
        if (retrievedDocuments == null) {
            return List.of();
        }
        return retrievedDocuments.stream().map(result -> {
            RagSource source = new RagSource();
            source.setSourceId(result.getDocument().getSourceId());
            source.setSourceType(result.getDocument().getSourceType());
            source.setTitle(result.getDocument().getTitle());
            source.setExcerpt(result.getDocument().excerpt(120));
            source.setScore(result.getScore());
            source.setKeywordScore(result.getKeywordScore());
            source.setVectorScore(result.getVectorScore());
            source.setRetrievalMode(result.getRetrievalMode());
            return source;
        }).collect(Collectors.toList());
    }

    private List<String> normalizeCitations(List<String> citations, List<RagSource> sources, Boolean cannotAnswer) {
        Set<String> allowedSourceIds = sources.stream()
                .map(RagSource::getSourceId)
                .collect(Collectors.toSet());
        // 只保留本次真实检索出来的引用，防止模型编造 sourceId。
        List<String> validCitations = nonNull(citations).stream()
                .filter(allowedSourceIds::contains)
                .distinct()
                .collect(Collectors.toList());
        if (validCitations.isEmpty() && !Boolean.TRUE.equals(cannotAnswer)) {
            return sources.stream()
                    .map(RagSource::getSourceId)
                    .limit(2)
                    .collect(Collectors.toList());
        }
        return validCitations;
    }

    private List<String> nonNull(List<String> values) {
        return values == null ? new ArrayList<>() : values;
    }

    private Integer normalizeConfidence(Integer confidence,
                                        List<RetrievedKnowledgeDocument> retrievedDocuments,
                                        Boolean cannotAnswer) {
        int clampedConfidence = clampConfidence(confidence);
        if (Boolean.TRUE.equals(cannotAnswer)) {
            if (confidence == null) {
                return 30;
            }
            return Math.max(30, Math.min(clampedConfidence, 50));
        }
        int minConfidence = minConfidenceFromRetrievalScore(bestScore(retrievedDocuments));
        return Math.max(clampedConfidence, minConfidence);
    }

    private int bestScore(List<RetrievedKnowledgeDocument> retrievedDocuments) {
        if (retrievedDocuments == null || retrievedDocuments.isEmpty()) {
            return 0;
        }
        return retrievedDocuments.stream()
                .mapToInt(RetrievedKnowledgeDocument::getScore)
                .max()
                .orElse(0);
    }

    private int minConfidenceFromRetrievalScore(int bestScore) {
        if (bestScore >= 30) {
            return 90;
        }
        if (bestScore >= 20) {
            return 85;
        }
        if (bestScore >= 10) {
            return 75;
        }
        if (bestScore > 0) {
            return 65;
        }
        return 30;
    }

    private Integer clampConfidence(Integer confidence) {
        if (confidence == null) {
            return 0;
        }
        if (confidence < 0) {
            return 0;
        }
        if (confidence > 100) {
            return 100;
        }
        return confidence;
    }
}
