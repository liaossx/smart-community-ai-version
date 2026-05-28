package com.lsx.ai.observability.model;

import java.util.List;
import java.util.UUID;

public class AiCallLogEntry {
    private final String requestId = UUID.randomUUID().toString().replace("-", "");
    private final long startNanos = System.nanoTime();
    private String bizType;
    private String bizKey;
    private String provider;
    private String providerVersion;
    private String model;
    private String status;
    private Integer confidence;
    private String requestSummary;
    private String responseSummary;
    private List<String> retrievedSourceIds;
    private String errorMessage;

    public static AiCallLogEntry start(String bizType) {
        AiCallLogEntry entry = new AiCallLogEntry();
        entry.bizType = bizType;
        return entry;
    }

    public AiCallLogEntry bizKey(String bizKey) {
        this.bizKey = bizKey;
        return this;
    }

    public AiCallLogEntry provider(String provider, String providerVersion, String model) {
        this.provider = provider;
        this.providerVersion = providerVersion;
        this.model = model;
        return this;
    }

    public AiCallLogEntry status(String status) {
        this.status = status;
        return this;
    }

    public AiCallLogEntry confidence(Integer confidence) {
        this.confidence = confidence;
        return this;
    }

    public AiCallLogEntry requestSummary(String requestSummary) {
        this.requestSummary = requestSummary;
        return this;
    }

    public AiCallLogEntry responseSummary(String responseSummary) {
        this.responseSummary = responseSummary;
        return this;
    }

    public AiCallLogEntry retrievedSourceIds(List<String> retrievedSourceIds) {
        this.retrievedSourceIds = retrievedSourceIds;
        return this;
    }

    public AiCallLogEntry error(Throwable error) {
        this.errorMessage = error == null ? null : error.getClass().getSimpleName() + ": " + error.getMessage();
        return this;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getBizType() {
        return bizType;
    }

    public String getBizKey() {
        return bizKey;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public String getModel() {
        return model;
    }

    public String getStatus() {
        return status;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public String getRequestSummary() {
        return requestSummary;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public List<String> getRetrievedSourceIds() {
        return retrievedSourceIds;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int latencyMs() {
        long elapsedNanos = System.nanoTime() - startNanos;
        return (int) Math.min(Integer.MAX_VALUE, elapsedNanos / 1_000_000L);
    }
}
