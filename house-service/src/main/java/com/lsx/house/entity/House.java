package com.lsx.house.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_house")
public class House {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long communityId;
    private String communityName;
    private String buildingNo;
    private String houseNo;
    private Integer floor;
    private Double area;
    private String type; // 户型
    private Integer bindStatus; // 0-未绑定 1-已绑定
    private Integer isDefault; // 是否默认房屋：1（是）、0（否）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
