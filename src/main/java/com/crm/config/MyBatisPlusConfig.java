package com.crm.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 * <p>
 * 注册分页插件等 MyBatis-Plus 拦截器
 */
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件：指定数据库类型为 MySQL
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor();
        // 单页最大条数限制（防止恶意请求一次性查百万条）
        pagination.setMaxLimit(500L);
        // 溢出处理：页码超出范围时返回第一项
        pagination.setOverflow(true);

        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
