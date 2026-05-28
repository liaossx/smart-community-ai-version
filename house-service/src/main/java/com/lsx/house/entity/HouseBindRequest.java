package com.lsx.house.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_house_bind_request")
public class HouseBindRequest {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String realName;
    private String phone;

    private Long houseId;
    private Long communityId;
    private String communityName;
    private String buildingNo;
    private String houseNo;

    private String identityType; // OWNER / FAMILY / TENANT
    private String status; // PENDING / APPROVED / REJECTED
    private LocalDateTime applyTime;

    private LocalDateTime approveTime;
    private Long approveBy;
    private String rejectReason;
}
