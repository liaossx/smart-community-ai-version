package com.lsx.ai.knowledge.dto;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeEmbeddingRebuildResponse {
    private String rebuildBatchNo;
    private Integer scannedCount;
    private Integer embeddedCount;
    private Integer skippedCount;
    private Integer failedCount;
    private String provider;
    private String model;
    private Integer dimension;
    private List<String> messages = new ArrayList<>();

    public String getRebuildBatchNo() {
        return rebuildBatchNo;
    }

    public void setRebuildBatchNo(String rebuildBatchNo) {
        this.rebuildBatchNo = rebuildBatchNo;
    }

    public Integer getScannedCount() {
        return scannedCount;
    }

    public void setScannedCount(Integer scannedCount) {
        this.scannedCount = scannedCount;
    }

    public Integer getEmbeddedCount() {
        return embeddedCount;
    }

    public void setEmbeddedCount(Integer embeddedCount) {
        this.embeddedCount = embeddedCount;
    }

    public Integer getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
