package com.lsx.core.common.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        // 设置响应格式和状态码（403 表示无权限）
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 状态码
        PrintWriter out = response.getWriter();
        // 友好错误提示（可根据业务调整文案）
        out.write("{\"code\":403,\"msg\":\"无对应角色权限，无法访问该接口\",\"data\":null}");
        out.flush();
        out.close();
    }
}