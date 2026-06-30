package com.crm.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 * <p>
 * 注册分页插件等 MyBatis-Plus 拦截器。
 *
 * @author CRM Team
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器
     * <p>
     * 核心功能：
     * 1. 分页插件 (PaginationInnerInterceptor)：自动拦截分页查询，
     *    根据数据库方言拼接 LIMIT 语句。
     * <p>
     * 扩展说明（后续可添加）：
     * - 乐观锁插件 (OptimisticLockerInnerInterceptor)
     * - 防全表更新/删除插件 (BlockAttackInnerInterceptor)
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件：指定数据库类型为 MySQL
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大条数限制（防止恶意请求一次性查百万条）
        pagination.setMaxLimit(500L);
        // 溢出处理：页码超出范围时返回第一页
        pagination.setOverflow(true);

        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
