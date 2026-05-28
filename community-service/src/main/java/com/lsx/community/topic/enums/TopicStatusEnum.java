package com.lsx.community.topic.enums;

import lombok.Getter;

@Getter
public enum TopicStatusEnum {
    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "审核通过"),
    REJECTED("REJECTED", "驳回");

    private final String code;
    private final String desc;

    TopicStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
