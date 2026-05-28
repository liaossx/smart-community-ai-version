package com.lsx.ai.customer.knowledge;

import java.util.Collections;
import java.util.List;

public class KnowledgeDocument {
    private final String sourceId;
    private final String sourceType;
    private final String title;
    private final String content;
    private final List<String> keywords;
    private final Long communityId;

    public KnowledgeDocument(String sourceId, String sourceType, String title, String content,
                             List<String> keywords, Long communityId) {
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.title = title;
        this.content = content;
        this.keywords = keywords == null ? Collections.emptyList() : keywords;
        this.communityId = communityId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public String excerpt(int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    public String toContextBlock(int index) {
        return String.join("\n",
                "[" + index + "] sourceId: " + sourceId,
                "sourceType: " + sourceType,
                "title: " + title,
                "content: " + content
        );
    }
}
