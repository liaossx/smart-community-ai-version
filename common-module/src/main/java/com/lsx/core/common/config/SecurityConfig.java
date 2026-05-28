package com.lsx.core.common.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Resource
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * Spring Security 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 核心安全配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("===== 加载 Security 配置：启用 JWT 登录校验 =====");

        http
                // 禁用 CSRF
                .csrf(csrf -> csrf.disable())

                // 使用无状态会话（JWT 必须这样）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 配置接口权限规则
                .authorizeHttpRequests(auth -> auth

                        /* ===== Swagger/Knife4j 放行 ===== */
                        .antMatchers(
                                "/doc.html",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/**/v3/api-docs",
                                "/v2/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        /* ===== 登录注册放行 ===== */
                        .antMatchers(
                                "/api/user/login",
                                "/api/user/register",
                                "/api/user/test/generateTokens", // 放行生成token的接口
                                "/api/common/**",
                                "/api/user/inner/**",
                                "/api/notice/inner/**",
                                "/api/system/config/inner/**",
                                "/api/fee/pay/callback",
                                "/api/fee/pay/callback/mock",
                                "/api/parking/pay/callback",
                                "/api/parking/pay/success",
                                "/api/house/list/ids",
                                "/api/house/info",
                                "/api/house/community/*/name"
                        ).permitAll()

                        /* ===== 管理员（ADMIN）可访问 ===== ⭐ 先配置具体的admin路径 */
                        .antMatchers("/api/repair/admin/**", "/api/repair/stats/**").hasAnyRole("ADMIN", "SUPER_ADMIN")  // ⭐ 报修统计及管理
                        .antMatchers("/api/workorder/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // ⭐ 工单管理
                        .antMatchers("/api/workorder/ai/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // AI work order assistant
                        .antMatchers("/api/admin/user/register-requests/**").hasRole("SUPER_ADMIN")
                        .antMatchers("/api/admin/user/**", "/api/admin/stats/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // ⭐ 用户管理及系统统计
                        .antMatchers("/api/admin/house/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .antMatchers("/api/house/admin/**", "/api/house/community/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .antMatchers("/api/system/config/**", "/api/monitor/operlog/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // 系统配置及日志
                        .antMatchers("/api/parking/order/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // 停车订单管理
                        .antMatchers("/api/parking/space/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // 车位管理
                        .antMatchers("/api/parking/reserve/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // 车位预订管理
                        .antMatchers("/api/vehicle/admin/**", "/api/parking/vehicle/admin/**", "/api/vehicle/audit/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // 车辆审核管理
                        .antMatchers("/api/house/updateUserHouseStatus").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .antMatchers("/api/fee/list", "/api/fee/generate", "/api/fee/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // 费用管理
                        
                        // 补充 访客、投诉、活动、公告 管理端统计及管理接口权限
                        .antMatchers("/api/visitor/list", "/api/visitor/admin/**", "/api/visitor/stats/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .antMatchers("/api/complaint/list", "/api/complaint/admin/**", "/api/complaint/stats/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .antMatchers("/api/notice/admin/**", "/api/notice/stats/**").hasAnyRole("ADMIN", "SUPER_ADMIN") // notice/list 移除管理员限制，供业主访问
                        .antMatchers("/api/activity/publish", "/api/activity/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // 允许所有已认证用户（包括业主和管理员）查看活动列表
                        .antMatchers("/api/activity/list").authenticated() 
                        .antMatchers(org.springframework.http.HttpMethod.GET, "/api/activity/{id}").authenticated()
                        
                        /* ===== 访客、投诉 业主端接口 ===== */
                        .antMatchers("/api/visitor/apply", "/api/visitor/my").hasRole("OWNER")
                        .antMatchers("/api/complaint/submit", "/api/complaint/my").hasRole("OWNER")
                        .antMatchers("/api/activity/join").hasRole("OWNER")
                        .antMatchers("/api/vehicle/bind").hasRole("OWNER")
                        .antMatchers("/api/workorder/worker/**").hasRole("WORKER") // 维修员操作
                        .antMatchers("/api/notice/list", "/api/notice/user/list", "/api/notice/unread-count", "/api/notice/*/read").hasAnyRole("OWNER", "ADMIN", "SUPER_ADMIN") // 公告读取：业主与管理员均可访问（控制器内会按角色处理）

                        /* ===== 普通业主（OWNER）可访问 ===== ⭐ 后配置通用路径 */
                        .antMatchers("/api/repair/**").hasRole("OWNER")  // ⭐ 放在后面！
                        .antMatchers("/api/user/bindUserToHouse").hasRole("OWNER")
                        .antMatchers("/api/house/bind").hasRole("OWNER")

                        /* ===== 其他接口需要登录 ===== */
                        .anyRequest().authenticated()
                )

                // 自定义无权限处理
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // 加载 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 禁用默认登录/注销
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }
}
