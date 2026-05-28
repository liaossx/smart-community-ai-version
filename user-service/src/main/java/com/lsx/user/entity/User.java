package com.lsx.user.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data  // Lombok娉ㄨВ锛氳嚜鍔ㄧ敓鎴恎etter/setter
@TableName("sys_user")  // 鍏宠仈鏁版嵁搴撹〃鍚?
public class User {
    @TableId(type = IdType.AUTO)  // 涓婚敭鑷锛堝搴旇〃鐨刬d瀛楁锛?
    private Long id;
    private String username;  // 鐧诲綍鐢ㄦ埛鍚?
    private String password;  // 鐧诲綍瀵嗙爜锛圡VP闃舵鏆傚瓨鏄庢枃锛?
    private String realName;  // 鐪熷疄濮撳悕
    private String phone;     // 鎵嬫満鍙?
    private String role;      // 瑙掕壊锛歰wner锛堜笟涓伙級銆乤dmin锛堢鐞嗗憳锛?
    private Long communityId; // 褰掑睘绀惧尯ID
    private Integer status;   // 鐘舵€侊細0-绂佺敤锛?-姝ｅ父
    private java.math.BigDecimal balance; // 璐︽埛浣欓
    private LocalDateTime createTime;  // 鍒涘缓鏃堕棿
    private LocalDateTime updateTime;  // 鏇存柊鏃堕棿
}
