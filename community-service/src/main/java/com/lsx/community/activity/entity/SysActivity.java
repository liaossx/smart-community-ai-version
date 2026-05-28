package com.lsx.community.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_activity")
public class SysActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long communityId;
    private String title;
    private String content;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;
    
    private String location;
    private Integer maxCount;
    private Integer signupCount;
    private String status;   //'PUBLISHED': '报名中','ONLINE': '进行中','ENDED': '已结束'#
    private String coverUrl;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
