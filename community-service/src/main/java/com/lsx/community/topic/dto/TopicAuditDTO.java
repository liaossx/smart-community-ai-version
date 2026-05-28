package com.lsx.community.topic.dto;

import lombok.Data;

@Data
public class TopicAuditDTO {
    private Long topicId;
    private String status; // APPROVED, REJECTED
    private Long adminId;
}
