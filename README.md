# 跨境电商客户资料管理系统 (CRM)

## 📋 项目概述

一个面向跨境电商业务场景的企业级客户资料录入与管理系统。适用于对接**店铺购买**、**开店培训**、**合作工厂**等多类型客户的日常管理需求。

### 核心功能

| 功能模块 | 说明 |
|---------|------|
| 客户信息录入 | 表单录入客户基本信息、业务类型、服务需求等 |
| 客户列表浏览 | 分页查询、多条件筛选、在线预览客户详情 |
| Excel 导出 | 一键导出客户数据为 Excel 文件 |
| 共享浏览 | Web端共享链接，团队成员可在线浏览客户信息 |

### 技术栈

| 层级 | 技术选型 |
|------|---------|
| 后端框架 | Spring Boot 2.7.x |
| 持久层 | MyBatis-Plus 3.5.x |
| 数据库 | MySQL 8.0 |
| 工具库 | Apache POI (Excel)、Lombok、Hutool |
| 前端 | HTML5 + CSS3 + 原生 JavaScript + Bootstrap 5 |
| 构建工具 | Maven 3.8+ |
| Java版本 | JDK 8/11/17 |

## 🚀 快速开始

### 环境要求

- JDK 8+
- Maven 3.8+
- MySQL 8.0+
- 现代浏览器（Chrome/Firefox/Edge）

### 1. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

### 2. 配置数据库连接

编辑 `backend/src/main/resources/application.yml`，修改数据库用户名和密码：

```yaml
spring:
  datasource:
    username: root
    password: your_password_here
```

### 3. 启动后端

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

后端默认启动在 `http://localhost:8080`

### 4. 打开前端

直接用浏览器打开 `frontend/index.html`，或使用 Live Server 等工具。

**注意**：如果后端端口不是8080，请修改 `frontend/js/api.js` 中的 `BASE_URL`。

## 📁 项目结构

```
Spring版客户资料录入项目/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 依赖配置
│   └── src/main/
│       ├── java/com/crm/
│       │   ├── CrmApplication.java       # 启动类
│       │   ├── entity/Customer.java      # 实体类
│       │   ├── dto/
│       │   │   ├── CustomerQueryDTO.java # 查询请求体
│       │   │   └── Result.java           # 统一响应体
│       │   ├── mapper/CustomerMapper.java
│       │   ├── service/
│       │   │   ├── CustomerService.java
│       │   │   └── impl/CustomerServiceImpl.java
│       │   ├── controller/CustomerController.java
│       │   ├── config/
│       │   │   ├── CorsConfig.java        # 跨域配置
│       │   │   └── MyBatisPlusConfig.java # 分页插件
│       │   └── util/ExcelUtil.java        # Excel导出工具
│       └── resources/application.yml
├── frontend/                          # 前端
│   ├── index.html                     # 主页面（客户列表）
│   ├── css/style.css                  # 样式
│   └── js/
│       ├── api.js                     # API 请求封装
│       └── app.js                     # 主逻辑
├── sql/init.sql                       # 数据库初始化脚本
├── DOC/                               # 项目文档
│   ├── 需求文档.md
│   ├── 技术文档.md
│   ├── 设计文档.md
│   └── API详细讲解文档.md
└── README.md                          # 本文件
```

## 🔧 配置说明

### application.yml 主要配置项

```yaml
server:
  port: 8080                    # 服务端口

spring:
  datasource:                   # 数据库连接
    url: jdbc:mysql://localhost:3306/crm_db
    username: root
    password: your_password
```

## 📮 API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/customers` | 新增客户 |
| PUT | `/api/customers/{id}` | 更新客户 |
| DELETE | `/api/customers/{id}` | 删除客户（逻辑删除） |
| GET | `/api/customers/{id}` | 查询单个客户 |
| POST | `/api/customers/list` | 分页+条件查询 |
| GET | `/api/customers/export` | 导出Excel |
| GET | `/api/customers/share` | 共享浏览（只读列表） |
| GET | `/api/customers/types` | 获取客户类型枚举 |

## 📚 文档索引

- [需求文档](DOC/需求文档.md)
- [技术文档](DOC/技术文档.md)
- [设计文档](DOC/设计文档.md)
- [API详细讲解文档](DOC/API详细讲解文档.md)

---

*本项目作为 Java 全栈实习生练手项目，按企业级标准组织代码结构。*
