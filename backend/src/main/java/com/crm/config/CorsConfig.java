package com.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域资源共享 (CORS) 配置
 * <p>
 * 前后端分离架构下，前端页面可能运行在不同端口/域名，
 * 浏览器会拦截跨域请求，需后端配置允许跨域。
 * <p>
 * 开发环境允许所有来源；生产环境应限制为具体域名。
 *
 * @author CRM Team
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 开发阶段允许所有来源，生产环境请改为具体域名
        config.addAllowedOriginPattern("*");
        // 允许携带 Cookie（如前后端需会话保持）
        config.setAllowCredentials(true);
        // 允许的 HTTP 方法
        config.addAllowedMethod("*");
        // 允许的请求头
        config.addAllowedHeader("*");
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用此 CORS 配置
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
