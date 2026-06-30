# MyBatis-Plus @MapperScan 扫描范围：不能扫 Service 包

## 问题

```
BindingException: Invalid bound statement (not found): com.crm.service.CustomerService.create
```

测试注入 `CustomerService` 后调用 `create()` 方法，MyBatis 报找不到 SQL 语句。

## 原因

`@MapperScan` 多写了 `com.crm.service`：

```java
// ❌ 错误：把 Service 包也扫进去了
@MapperScan("com.crm.mapper,com.crm.service")
```

MyBatis-Plus 会把 `com.crm.service` 下的 `CustomerService` **接口**也当成 Mapper，创建一个 JDK 动态代理并注册到 Spring 容器。此时容器里有两个 `CustomerService` 类型的 Bean：

| Bean | 来源 | 类型 |
|------|------|------|
| `CustomerServiceImpl` | `@Service` → Spring `@ComponentScan` | 真正的业务 Bean |
| MyBatis JDK 代理 | `CustomerService` 接口 → `@MapperScan` 误扫 | 假的 Mapper Bean（无 SQL 实现） |

Spring 注入时选中了假的 Mapper 代理，调用 `create()` → MyBatis 找不到对应 SQL → 报错。

## 解决

`@MapperScan` 只填 Mapper 包：

```java
// ✅ 正确：只扫 Mapper 接口所在的包
@MapperScan("com.crm.mapper")
```

## 核心知识点：两种扫描的区别

Spring Boot 启动时有两种独立的扫描机制，各管各的：

| | Spring Bean 扫描 | MyBatis Mapper 扫描 |
|------|------|------|
| **注解** | `@ComponentScan`（`@SpringBootApplication` 内置） | `@MapperScan("包路径")` |
| **扫什么** | 标注了 `@Service`、`@Component`、`@Repository` 的**类** | 指定包下的所有**接口** |
| **产出** | 普通的 Spring Bean | MyBatis JDK 动态代理（自动生成 SQL） |
| **用途** | 业务逻辑 | 数据库操作 |

```
@MapperScan 扫到的接口 → MyBatis 代理 → 调用时必须能找到 SQL（XML 或 BaseMapper 内置方法）
@MapperScan 没扫到的接口 → Spring 管理 → 调用时走实现的业务代码
```

## 规律

> **`@MapperScan` 只能填 Mapper 接口所在的包，不能填 Service 接口的包。两条线是独立的：Mapper 归 MyBatis 管，Service 归 Spring 管。**

```
src/main/java/com/crm/
├── mapper/
│   └── CustomerMapper.java      ← @MapperScan("com.crm.mapper") ✅
├── service/
│   ├── CustomerService.java     ← 由 @Service + @ComponentScan 管理 ✅
│   └── impl/
│       └── CustomerServiceImpl.java
└── entity/
    └── Customer.java
```
