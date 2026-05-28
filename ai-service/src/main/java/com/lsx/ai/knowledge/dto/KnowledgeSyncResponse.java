package com.lsx.ai.knowledge.dto;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeSyncResponse {
    private String syncBatchNo;
    private Integer scannedCount;
    private Integer syncedCount;
    private Integer disabledCount;
    private Integer failedCount;
    private List<String> messages = new ArrayList<>();

    public String getSyncBatchNo() {
        return syncBatchNo;
    }

    public void setSyncBatchNo(String syncBatchNo) {
        this.syncBatchNo = syncBatchNo;
    }

    public Integer getScannedCount() {
        return scannedCount;
    }

    public void setScannedCount(Integer scannedCount) {
        this.scannedCount = scannedCount;
    }

    public Integer getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(Integer syncedCount) {
        this.syncedCount = syncedCount;
    }

    public Integer getDisabledCount() {
        return disabledCount;
    }

    public void setDisabledCount(Integer disabledCount) {
        this.disabledCount = disabledCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
