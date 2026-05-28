package com.lsx.ai.knowledge.model;

public class KnowledgeChunkForEmbedding {
    private Long documentId;
    private Long chunkId;
    private String sourceType;
    private String sourceId;
    private Long communityId;
    private String title;
    private String content;
    private String keywords;
    private String contentHash;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String embeddingText() {
        return String.join("\n",
                "title: " + nullToEmpty(title),
                "content: " + nullToEmpty(content),
                "keywords: " + nullToEmpty(keywords)
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
