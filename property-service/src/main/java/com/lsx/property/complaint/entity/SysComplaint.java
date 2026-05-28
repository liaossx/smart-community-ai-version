package com.lsx.property.complaint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_complaint")
public class SysComplaint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long communityId;
    private String type;
    private String content;
    private String images;
    private String status;
    private String result;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
    
    public String getImgs() {
        return images;
    }
    
    public void setImgs(String imgs) {
        this.images = imgs;
    }
}
