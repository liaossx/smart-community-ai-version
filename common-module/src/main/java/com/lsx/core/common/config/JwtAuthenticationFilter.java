package com.lsx.core.common.config;

import com.lsx.core.common.Util.JwtUtil;
import com.lsx.core.common.Util.UserContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/doc.html",
            "/doc.html/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/v2/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/META-INF.resources/**",
            "/api/user/login",
            "/api/user/register",
            "/api/common/**",
            "/api/user/inner/**",
            "/api/notice/inner/**",
            "/api/system/config/inner/**",
            "/api/house/info",
            "/api/house/community/*/name",
            "/api/house/list/ids",
            "/api/fee/pay/callback",
            "/api/fee/pay/callback/mock",
            "/api/parking/pay/callback",
            "/api/parking/pay/success"
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // ===== 白名单放行 =====
        for (String whitePath : WHITE_LIST) {
            if (pathMatcher.match(whitePath, requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String token = request.getHeader("Authorization");

        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            if (token == null || token.trim().isEmpty()) {
                // 如果是白名单路径，直接放行，不抛异常
                boolean isWhite = false;
                for (String whitePath : WHITE_LIST) {
                    if (pathMatcher.match(whitePath, requestURI)) {
                        isWhite = true;
                        break;
                    }
                }
                if (isWhite) {
                    filterChain.doFilter(request, response);
                    return;
                }
                throw new RuntimeException("请先登录（Token 不存在）");
            }
            
            // 验证Token
            if (!jwtUtil.validateToken(token)) {
                 throw new RuntimeException("Token 已过期或无效");
            }

            // ---- 1. 从 token 中解析 userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                UserContext.setUserId(userId);
            }
            
            // ---- 1.1 解析 role 和 communityId 并设置到 UserContext
            String roleStr = jwtUtil.getRoleFromToken(token);
            UserContext.setRole(roleStr);
            
            Long communityId = jwtUtil.getCommunityIdFromToken(token);
            if (communityId != null) {
                UserContext.setCommunityId(communityId);
            }

            // ---- 2. 解析用户名
            String username = jwtUtil.getUsernameFromToken(token);
            if (username != null) {
                UserContext.setUsername(username);
            }
            
            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("Token 中未包含有效用户名");
            }

            // ---- 5. 设置权限
            String role = roleStr;
            
            String authority;
            if (role == null || role.trim().isEmpty()) {
                authority = "ROLE_USER"; // 默认角色
            } else {
                String normalizedRole = role.trim();
                if (normalizedRole.startsWith("ROLE_")) {
                    normalizedRole = normalizedRole.substring("ROLE_".length());
                }
                authority = "ROLE_" + normalizedRole.toUpperCase();
            }

            List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(authority)
            );

            Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // ---- 6.继续执行
            filterChain.doFilter(request, response);

        } catch (RuntimeException e) {
            // 认证相关异常，返回 401
            logger.warn("认证失败: " + e.getMessage());
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PrintWriter out = response.getWriter();
            out.write("{\"code\":401,\"msg\":\"" + e.getMessage() + "\",\"data\":null}");
            out.flush();
            out.close();
        } catch (Exception e) {
            // 其他未知异常（如SQL报错、空指针等），打印堆栈并返回 500
            logger.error("系统内部异常", e);
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            out.write("{\"code\":500,\"msg\":\"系统内部错误：" + e.getMessage() + "\",\"data\":null}");
            out.flush();
            out.close();
        } finally {
            UserContext.clear();
        }
    }
}
