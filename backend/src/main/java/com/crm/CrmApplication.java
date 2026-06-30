package com.crm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 跨境电商客户资料管理系统 - 启动类
 *
 * @author CRM Team
 * @since 2026-06-30
 */
@SpringBootApplication
@MapperScan("com.crm.mapper")
public class CrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
        System.out.println("========================================");
        System.out.println("  客户资料管理系统启动成功！");
        System.out.println("  API 地址: http://localhost:8080");
        System.out.println("========================================");
    }
}
