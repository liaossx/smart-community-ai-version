package com.lsx.house.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_house")
public class UserHouse {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long houseId;
    // 状态：0-待审核 1-审核通过 2-审核拒绝 (部分旧代码可能混用了数字字符串和中文)
    private String status;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private LocalDateTime updateTime;
}
