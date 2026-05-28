package com.lsx.core.common.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairMsgDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 报修单ID
     */
    private Long repairId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 故障类型
     */
    private String faultType;

    /**
     * 提交时间
     */
    private LocalDateTime createTime;
}
