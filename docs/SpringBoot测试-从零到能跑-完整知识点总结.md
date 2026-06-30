# Spring Boot 测试：从零到能跑 — 完整知识点总结

> 学习方式：遇到问题 → 定位根因 → 总结规律 → 形成系统认知

---

## 一、测试类的包路径规则

### 遇到的问题

```
Could not autowire. No beans of 'CustomerService' type found.
```

### 根因

测试类放在 `src/test/java/` 根目录（默认包），没有 `package` 声明。Spring Boot 的 `@SpringBootTest` 从测试类所在包向上查找 `@SpringBootApplication` 启动类，默认包无法向上查找 → Spring 容器没启动 → 扫描不到任何 Bean。

### 知识点

**规则**：测试类的包路径必须和启动类在同一个包体系下（启动类包名或其子包）。

```
启动类：com.crm.CrmApplication

✅ src/test/java/com/crm/service/CustomerServiceTest.java  → package com.crm.service
✅ src/test/java/com/crm/CustomerServiceTest.java          → package com.crm
❌ src/test/java/CustomerServiceTest.java                  → 无 package（默认包）
```

**对照表**：main 里文件放哪，test 里文件就放哪。

| main 代码路径 | package | test 代码路径 | package |
|-------------|---------|-------------|---------|
| `main/java/com/crm/service/CustomerService.java` | `com.crm.service` | `test/java/com/crm/service/CustomerServiceTest.java` | `com.crm.service` |
| `main/java/com/crm/controller/XxxController.java` | `com.crm.controller` | `test/java/com/crm/controller/XxxControllerTest.java` | `com.crm.controller` |

**原理**：`@SpringBootTest` 默认从测试类所在包开始，逐级向上查找标注了 `@SpringBootApplication`（或 `@SpringBootConfiguration`）的类，找到后才启动 Spring 容器。如果测试类在默认包，Java 的无名包和具名包之间是隔离的，无法找到启动类。

---

## 二、测试配置文件隔离

### 遇到的问题

```
Failed to bind properties under 'logging.level' to Map<String, LogLevel>
BindingException: Invalid bound statement (not found): com.crm.service.CustomerService.create
```

### 根因

1. **YAML 格式错误**：`key:value`（冒号后无空格）被 YAML 解析为普通字符串，而非键值对
2. **测试没有独立配置**：`src/test/resources/` 目录不存在，测试直接加载 `main` 的 `application.yml`，连接真实 MySQL。测试环境的 MyBatis-Plus 初始化异常导致 Service 接口被误当成 Mapper 去解析

### 知识点

**规则 1 — YAML 语法**：冒号表示键值分隔时，后面必须跟一个空格。

```yaml
# ❌ 错误：冒号后无空格 → 整个 "com.crm:debug" 被当作一个字符串
logging:
  level:
    com.crm:debug

# ✅ 正确：冒号后有空格 → key=com.crm, value=debug
logging:
  level:
    com.crm: debug
```

**规则 2 — 测试配置覆盖**：`src/test/resources/` 下的配置文件会**覆盖** `src/main/resources/` 下的同名文件。这是 Spring Boot 的资源加载优先级决定的（test classpath > main classpath）。

```
项目结构：
src/
├── main/resources/
│   └── application.yml        ← 生产配置（MySQL）
└── test/resources/
    └── application.yml        ← 测试配置（H2，覆盖上面的）
```

**规则 3 — 测试用内存数据库**：测试不应该依赖外部 MySQL，应使用 H2 内存数据库。

```yaml
# src/test/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:mem:crm_test;DB_CLOSE_DELAY=-1;MODE=MySQL
    username: sa
    password:
    driver-class-name: org.h2.Driver
```

| 参数 | 含义 |
|------|------|
| `mem:crm_test` | 内存数据库，名称 crm_test |
| `DB_CLOSE_DELAY=-1` | JVM 退出前不关闭数据库（测试期间保持连接） |
| `MODE=MySQL` | 模拟 MySQL 语法（兼容 MyBatis-Plus 的 SQL） |

**规则 4 — Spring Boot 配置加载优先级**（从高到低）：

1. 命令行参数 `--server.port=9090`
2. 操作系统环境变量
3. `application-{profile}.yml`（profile 特定）
4. `application.yml`
5. `@PropertySource` 注解

测试环境下，`test/resources/application.yml` 的优先级高于 `main/resources/application.yml`。

---

## 三、Lombok @Builder 类型严格匹配

### 遇到的问题

```
'Customer(Long, String, ...)' cannot be applied to '(int, String, ...)'
```

### 根因

`@Builder` 生成的构造器每个字段方法是**强类型**的，`int` 不会自动转换为 `Long`。传了 `1`（int 类型）给 `id(Long)` 方法。

### 知识点

| 实体字段类型 | 错误写法 | 正确写法 |
|-----------|---------|---------|
| `Long id` | `.id(1)` → `int` | `.id(1L)` → `long` → 自动装箱 `Long` |
| `Integer count` | `.count(1L)` → `long` | `.count(1)` → `int` |
| `LocalDateTime time` | `.time("2024-01-01")` → `String` | `.time(LocalDateTime.now())` |
| `Float rate` | `.rate(0.5)` → `double` | `.rate(0.5F)` → `float` |

**规律**：Lombok Builder 不讲类型兼容，只看类型**一模一样**。Java 的基本类型不会自动转换（`int` → `Long` 不是自动装箱，是不同类型）。

**最佳实践**：自增主键不设 `id`，让数据库自动生成。

---

## 四、MyBatis-Plus 测试环境要点

### 知识点

**`@MapperScan` 的作用**：告诉 MyBatis-Plus 扫描哪个包下的 Mapper 接口，为它们创建代理对象（动态生成 SQL）。

```java
@SpringBootApplication
@MapperScan("com.crm.mapper")  // 只扫描 com.crm.mapper 包
public class CrmApplication { ... }
```

**Mapper 代理的生命周期**：

```
@MapperScan → 扫描 Mapper 接口 → 创建 SqlSessionFactory 
→ 为每个 Mapper 接口生成 JDK 动态代理 
→ 代理对象注册到 Spring 容器 → Service 通过构造函数注入
```

**测试中 MyBatis-Plus 的常见坑**：

| 问题 | 原因 | 解决 |
|------|------|------|
| `BindingException: statement not found` | Mapper 代理初始化失败（通常因为数据源问题） | 用 H2 替换 MySQL |
| Mapper Bean 无法注入 | `@MapperScan` 路径配错 | 核对包路径 |
| 表不存在 | H2 默认不自动建表 | 添加 `schema.sql` 或让 MyBatis-Plus 自动 DDL |

---

## 五、完整测试类模板

```java
package com.crm.service;  // ← 1. 包路径和 main 一致

import com.crm.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest  // ← 2. 启动完整 Spring 容器
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;  // ← 3. 注入真实 Bean

    @Test
    void shouldCreateCustomer() {
        // 4. 用 Builder 构造数据，注意类型匹配
        Customer customer = Customer.builder()
                .companyName("测试公司")
                .contactPerson("张三")
                .phone("13800138000")
                .customerType("店铺购买")
                .build();  // 自增ID不设值

        // 5. 调用 + 断言
        Customer result = customerService.create(customer);
        assertThat(result.getId()).isNotNull();
    }
}
```

---

## 六、项目测试环境文件清单

```
src/test/
├── java/com/crm/service/
│   └── CustomerServiceTest.java    # 测试类（package 和 main 一致）
└── resources/
    └── application.yml             # 测试专用配置（H2 数据源）
```

| 文件 | 作用 | 必须？ |
|------|------|-------|
| `test/java/.../XxxTest.java` | 测试代码 | ✅ 必须 |
| `test/resources/application.yml` | 覆盖 main 配置 | ✅ 强烈建议（隔离外部依赖） |
| `pom.xml` 中 H2 依赖（`scope=test`） | 提供内存数据库 | ✅ 必须 |

---

## 核心心法

> **测试和 main 是一一对应的镜像关系**：
> - main 有 package，test 也要有相同的 package
> - main 有 application.yml（MySQL），test 也要有 application.yml（H2 覆盖）
> - main 的 Bean 在测试中通过 `@Autowired` 注入，但前提是包路径正确、配置正确、Spring 容器能正常启动
