package com.lsx.community.group.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupMemberVO {
    private Long userId;
    private String userName;
    private Integer buyCount;
    private LocalDateTime createTime;
}
