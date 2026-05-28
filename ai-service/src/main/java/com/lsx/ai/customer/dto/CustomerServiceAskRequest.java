package com.lsx.ai.customer.dto;

import jakarta.validation.constraints.NotBlank;

public class CustomerServiceAskRequest {
    private Long communityId;

    @NotBlank(message = "question is required")
    private String question;

    private Integer topK;

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
