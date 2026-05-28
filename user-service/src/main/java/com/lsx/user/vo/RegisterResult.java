package com.lsx.user.vo;

import lombok.Data;


import java.time.LocalDateTime;
@Data
public class RegisterResult {
    private Long id;
    private String username;  // 鐧诲綍鐢ㄦ埛鍚?
    private String realName;  // 鐪熷疄濮撳悕
    private String phone;     // 鎵嬫満鍙?
    private String role;      // 瑙掕壊锛歰wner锛堜笟涓伙級銆乤dmin锛堢鐞嗗憳锛?
    private LocalDateTime createTime;  // 鍒涘缓鏃堕棿
}

