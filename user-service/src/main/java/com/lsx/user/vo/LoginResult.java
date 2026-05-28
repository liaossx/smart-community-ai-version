package com.lsx.user.vo;

import com.lsx.user.entity.User;
import lombok.Data;

@Data
public class LoginResult {
    private Long userId;
    private String username;
    private String token;
    private String role; // 鐢ㄦ埛瑙掕壊
    private Long communityId; // 绀惧尯ID
    private String tokenType = "Bearer"; // Token绫诲瀷

    public LoginResult(User user, String token) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.token = token;
        this.role = user.getRole(); // 鍋囪User瀹炰綋鏈塯etRole鏂规硶
        this.communityId = user.getCommunityId();
    }
}
