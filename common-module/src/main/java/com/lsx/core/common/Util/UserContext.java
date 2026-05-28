package com.lsx.core.common.Util;

public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> COMMUNITY_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getCurrentUserId() {
        return USER_ID.get();
    }

    public static void setCommunityId(Long communityId) {
        COMMUNITY_ID.set(communityId);
    }

    public static Long getCommunityId() {
        return COMMUNITY_ID.get();
    }

    public static void setRole(String role) {
        ROLE.set(role);
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static String getUsername() {
        return USERNAME.get();
    }
    
    // 兼容旧代码里可能用到的方法名
    public static Long getUserId() {
        return USER_ID.get();
    }
    
    public static String getUserRole() {
        return ROLE.get();
    }
    
    public static Long getUserCommunityId() {
        return COMMUNITY_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
        COMMUNITY_ID.remove();
        ROLE.remove();
        USERNAME.remove();
    }
}
