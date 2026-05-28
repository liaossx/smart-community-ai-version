package com.lsx.core.common.enums;

public enum HouseStatusEnum {

    PENDING_REVIEW("待审核"),
    APPROVED("审核通过"),
    REJECTED("审核不通过");

    // 2. 存储状态对应的文字（数据库实际存储的内容）
    private final String statusText;

    // 3. 构造器（枚举类构造器默认private，无需手动写）
    HouseStatusEnum(String statusText) {
        this.statusText = statusText;
    }

    // 4. getter方法：获取状态文字（供业务层/控制层使用）
    public String getStatusText() {
        return statusText;
    }

    // 5. 核心校验方法：根据传入的文字，判断是否为合法状态
    public static void validateStatus(String statusText) {
        //非空校验
        if (statusText == null || statusText.trim().isEmpty()) {
            throw new RuntimeException("房屋状态不能为空");
        }
        Boolean isLegal = false;
        //遍历所有枚举型 ，看传入的值是否匹配
        for (HouseStatusEnum status : values()) {
            if (status.getStatusText().equals(statusText.trim())) {
                isLegal = true;
                break;
            }

        }
        if (!isLegal) {
        throw new RuntimeException( "非法房屋状态：" + statusText + "，合法状态为：待审核、审核通过、审核不通过");
        }
        }
    }

