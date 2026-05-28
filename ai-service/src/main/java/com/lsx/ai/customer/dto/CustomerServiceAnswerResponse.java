package com.lsx.ai.customer.dto;

import java.util.ArrayList;
import java.util.List;

public class CustomerServiceAnswerResponse {
    private String answer;
    private List<String> followUpActions = new ArrayList<>();
    private List<String> citations = new ArrayList<>();
    private Boolean cannotAnswer;
    private Integer confidence;
    private String provider;
    private String providerVersion;
    private String model;
    private List<RagSource> sources = new ArrayList<>();

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getFollowUpActions() {
        return followUpActions;
    }

    public void setFollowUpActions(List<String> followUpActions) {
        this.followUpActions = followUpActions;
    }

    public List<String> getCitations() {
        return citations;
    }

    public void setCitations(List<String> citations) {
        this.citations = citations;
    }

    public Boolean getCannotAnswer() {
        return cannotAnswer;
    }

    public void setCannotAnswer(Boolean cannotAnswer) {
        this.cannotAnswer = cannotAnswer;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public void setProviderVersion(String providerVersion) {
        this.providerVersion = providerVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<RagSource> getSources() {
        return sources;
    }

    public void setSources(List<RagSource> sources) {
        this.sources = sources;
    }
}
