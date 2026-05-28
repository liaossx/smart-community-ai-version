package com.lsx.ai.customer.knowledge;

public class RetrievedKnowledgeDocument {
    private final KnowledgeDocument document;
    private final int score;
    private final int keywordScore;
    private final int vectorScore;
    private final String retrievalMode;

    public RetrievedKnowledgeDocument(KnowledgeDocument document, int score) {
        this(document, score, score, 0, "KEYWORD");
    }

    public RetrievedKnowledgeDocument(KnowledgeDocument document, int score,
                                      int keywordScore, int vectorScore, String retrievalMode) {
        this.document = document;
        this.score = score;
        this.keywordScore = Math.max(keywordScore, 0);
        this.vectorScore = Math.max(vectorScore, 0);
        this.retrievalMode = retrievalMode;
    }

    public KnowledgeDocument getDocument() {
        return document;
    }

    public int getScore() {
        return score;
    }

    public int getKeywordScore() {
        return keywordScore;
    }

    public int getVectorScore() {
        return vectorScore;
    }

    public String getRetrievalMode() {
        return retrievalMode;
    }
}
