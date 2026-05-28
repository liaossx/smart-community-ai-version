package com.lsx.property.notice.dto;

import lombok.Data;

@Data
public class NoticeListDTO {
    private Long id;
    private String title;
    private Integer readFlag; // 0=閺堫亣顕伴敍?=瀹歌尪顕?    private String publishTime; // yyyy-MM-dd HH:mm:ss閺嶇厧绱?    private String targetType;
    // 閸欘垶鈧绱版穱婵堟殌content閻劋绨拠锔藉剰妞?    private String content;
}
