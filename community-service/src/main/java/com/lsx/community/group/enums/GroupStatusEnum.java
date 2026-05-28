package com.lsx.community.group.enums;

import lombok.Getter;

@Getter
public enum GroupStatusEnum {
    ONGOING("ONGOING", "进行中"),
    FINISHED("FINISHED", "已结束"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    GroupStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
