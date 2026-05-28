package com.lsx.property.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通知过期设置DTO")
public class NoticeExpireDTO {

    @Schema(description = "过期类型", required = true)
    private ExpireType expireType;

    public enum ExpireType {
        NEVER("永不过期"),
        CUSTOM("自定义时间"),
        DAYS_7("7天后"),
        DAYS_30("30天后"),
        MONTH_3("3个月后");

        private final String desc;

        ExpireType(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
    }
}
