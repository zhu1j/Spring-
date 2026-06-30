# API实现原理与技术栈拆解文档

> **版本**：v1.0.0  
> **日期**：2026-06-30  
> **面向读者**：Java 实习生、初中级开发者  
> **前置阅读**：《API详细讲解文档》

---

## 什么是本文档？

**本文档的定位**：我们实现了 8 个定制化业务 API（新增客户、分页查询、Excel导出等）。每个功能的背后，实际上是**组合调用**了 JDK 内置类、Spring 框架、MyBatis-Plus、Apache POI 等第三方库提供的现成 API。本文档把每一条调用链路**拆解到方法级别**，告诉你：

- 这个方法**来自哪个 jar 包、哪个类**；
- 它的**完整签名**是什么（入参类型 + 返回值类型）；
- 在我们的业务场景中，**每个参数的具体含义**；
- 多个底层 API 之间是怎么**串联**成完整功能的。

读完本文档，你将能够从"会用框架"进阶到"理解框架在干什么"。

---

## 目录

1. [技术栈分层速览](#1-技术栈分层速览)
2. [前置知识：贯穿全局的底层 API](#2-前置知识贯穿全局的底层-api)
3. [8 个接口逐一切割拆解](#3-8-个接口逐一切割拆解)
   - [3.1 POST /api/customers — 新增客户](#31-post-apicustomers--新增客户)
   - [3.2 PUT /api/customers/{id} — 更新客户](#32-put-apicustomersid--更新客户)
   - [3.3 DELETE /api/customers/{id} — 删除客户](#33-delete-apicustomersid--删除客户)
   - [3.4 GET /api/customers/{id} — 查询详情](#34-get-apicustomersid--查询详情)
   - [3.5 POST /api/customers/list — 分页条件查询](#35-post-apicustomerslist--分页条件查询)
   - [3.6 GET /api/customers/export — Excel导出](#36-get-apicustomersexport--excel导出)
   - [3.7 POST /api/customers/share — 共享浏览](#37-post-apicustomersshare--共享浏览)
   - [3.8 GET /api/customers/types — 枚举查询](#38-get-apicustomerstypes--枚举查询)
4. [横切关注点拆解](#4-横切关注点拆解)
5. [底层API速查索引](#5-底层api速查索引)

---

## 1. 技术栈分层速览

```
┌──────────────────────────────────────────────────────────────┐
│ 第0层  JDK 标准库                                            │
│ java.util.*  java.time.*  java.io.*  java.net.*             │
├──────────────────────────────────────────────────────────────┤
│ 第1层  Servlet 规范 (javax.servlet)                          │
│ HttpServletResponse                                         │
├──────────────────────────────────────────────────────────────┤
│ 第2层  Spring Framework                                     │
│ spring-web  spring-context  spring-beans                    │
├──────────────────────────────────────────────────────────────┤
│ 第3层  Spring Boot                                          │
│ spring-boot-autoconfigure  spring-boot-starter-validation   │
├──────────────────────────────────────────────────────────────┤
│ 第4层  第三方增强库                                          │
│ MyBatis-Plus  Lombok  Jackson  Apache POI                   │
├──────────────────────────────────────────────────────────────┤
│ 第5层  我们写的代码 (定制层)                                   │
│ CrmApplication  Controller  Service  Mapper  Entity  DTO    │
│ Config  Util                                                │
└──────────────────────────────────────────────────────────────┘
```

> **核心原则**：你的代码越靠近第5层，就越接近业务。下面的拆解把每一行定制代码"翻译"成它实际调用的第0~4层的 API。

---

## 2. 前置知识：贯穿全局的底层 API

在逐一拆解 8 个接口之前，先认识几个**被所有接口共同依赖**的基础 API。这些 API 理解一次，后面就都能看懂。

### 2.1 Lombok 注解（编译期代码生成）

这些注解**在编译时自动生成 getter/setter/构造器代码**，不是运行时调用。

| 注解 | 来源 jar | 作用 | 生成的代码 |
|------|---------|------|-----------|
| `@Data` | `lombok` | 标记在类上 | 生成 `getXxx()` / `setXxx()` / `toString()` / `equals()` / `hashCode()` |
| `@Builder` | `lombok` | 标记在类上 | 生成静态内部类 `Builder`，支持链式构建对象 |
| `@NoArgsConstructor` | `lombok` | 标记在类上 | 生成无参构造器 `public Customer() {}` |
| `@AllArgsConstructor` | `lombok` | 标记在类上 | 生成全参构造器 |
| `@RequiredArgsConstructor` | `lombok` | 标记在类上 | 为 `final` 字段生成构造器（Spring 依赖注入用） |
| `@Slf4j` | `lombok` | 标记在类上 | 生成 `log` 静态字段 → 等价于 `Logger log = LoggerFactory.getLogger(Xxx.class)` |

> **关键理解**：你写了 `customer.getCompanyName()`，但 `getCompanyName()` 这个方法是 Lombok 根据 `@Data` **编译时自动生成**的，源码里你并看不到它。这就是 Lombok 的价值 — 省去手写样板代码。

### 2.2 Spring 依赖注入 — `@RequiredArgsConstructor`

```java
// 我们的代码：
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerMapper customerMapper;
}

// 等价于手写：
public class CustomerServiceImpl implements CustomerService {
    private final CustomerMapper customerMapper;

    // Spring 看到这个构造器，会自动从容器中找到 CustomerMapper 的 Bean 并传入
    public CustomerServiceImpl(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }
}
```

| 底层机制 | 来自 | 说明 |
|---------|------|------|
| Spring IoC 容器 | `spring-beans` | 启动时扫描 `@Mapper`、`@Service`，创建单例 Bean 存入容器 |
| 构造器注入 | `spring-beans` | 当类只有一个构造器时，Spring 自动用该构造器注入所有参数 |

### 2.3 `Result<T>` 静态工厂方法的泛型推导

```java
// 我们的代码：
Result.ok("客户录入成功", saved);

// 实际调用链路：
public static <T> Result<T> ok(String message, T data) {  // ① 泛型方法声明
    return Result.<T>builder()      // ② 显式指定 builder 的泛型
            .code(200)              // ③ @Builder 生成的链式调用
            .message(message)       // ④ 传入 String
            .data(data)             // ⑤ 传入 T 类型（由调用方推断）
            .timestamp(System.currentTimeMillis())  // ⑥ JDK 方法
            .build();               // ⑦ @Builder 生成的终结方法
}
```

| 步骤 | 方法 | 来自 | 说明 |
|------|------|------|------|
| ① | `<T> Result<T> ok(String, T)` | 我们写的 | 泛型方法，`<T>` 声明类型变量，`T data` 用实际类型替换 |
| ② | `Result.<T>builder()` | Lombok `@Builder` 生成 | 创建 Builder 实例，`<T>` 显式传递泛型（避免类型丢失） |
| ③ | `builder.code(200)` | Lombok 生成 | 设置 code 字段 |
| ④ | `builder.message(message)` | Lombok 生成 | 设置 message 字段 |
| ⑤ | `builder.data(data)` | Lombok 生成 | 设置 data 字段 |
| ⑥ | `System.currentTimeMillis()` | **JDK** `java.lang.System` | 返回 `long`：当前 UTC 毫秒时间戳（自 1970-01-01） |
| ⑦ | `builder.build()` | Lombok `@Builder` 生成 | 调用全参构造器创建 `Result<T>` 对象并返回 |

---

## 3. 8 个接口逐一切割拆解

---

### 3.1 POST `/api/customers` — 新增客户

#### 3.1.1 调用链路全景图

```
HTTP POST /api/customers  (JSON Body)
        │
        ▼
┌─────────────────────────────────────────────────────┐
│  Spring DispatcherServlet                           │
│  · 解析 URL → 匹配 @PostMapping                    │
│  · 读取 HTTP Body → JSON 反序列化                  │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│  CustomerController.create()                        │
│  ① @Valid 触发 JSR-303 校验                         │
│  ② log.info() — Slf4j 日志                          │
│  ③ customerService.create(customer)                 │
│  ④ Result.ok("客户录入成功", saved)                 │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│  CustomerServiceImpl.create()                       │
│  ① StringUtils.isBlank()  — 默认值处理              │
│  ② customerMapper.insert(customer)                  │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│  BaseMapper.insert(entity)                          │
│  MyBatis-Plus → JDBC → MySQL                        │
└─────────────────────────────────────────────────────┘
```

#### 3.1.2 逐层拆解

**第一层 — Spring MVC 注解匹配**

```java
@PostMapping                    // ①
public Result<Customer> create(
    @RequestBody                // ②
    @Valid                      // ③
    Customer customer           // ④
) {
```

| 标注 | 完整类名 | 来自 jar | 机制解释 |
|------|---------|---------|---------|
| ① `@PostMapping` | `org.springframework.web.bind.annotation.PostMapping` | `spring-web` | 等价于 `@RequestMapping(method = RequestMethod.POST)`。Spring DispatcherServlet 收到请求后，遍历所有 Controller 的方法，匹配 HTTP 方法 + URL 路径，找到此方法执行。 |
| ② `@RequestBody` | `org.springframework.web.bind.annotation.RequestBody` | `spring-web` | 触发 **HttpMessageConverter** 链：读取 HTTP Body 的字节流 → 根据 `Content-Type: application/json` 选择 `MappingJackson2HttpMessageConverter` → 调用 **Jackson** 的 `ObjectMapper.readValue()` 将 JSON 字符串反序列化为 `Customer` 对象。 |
| ③ `@Valid` | `javax.validation.Valid` | `jakarta.validation-api` (JDK 自带) | 标记"此参数需要校验"。Spring 检测到该注解后，自动调用 **Hibernate Validator** 的 `Validator.validate()` 方法，检查 `Customer` 类中所有 JSR-303 注解（`@NotBlank`, `@Email`）。校验失败抛出 `MethodArgumentNotValidException`。 |
| ④ `Customer customer` | 我们写的实体类 | — | 参数类型。Spring 通过反射确认方法签名，找到匹配的参数注入方式。 |

**第二层 — JSR-303 参数校验注解**

这些注解写在 `Customer.java` 的字段上：

```java
@NotBlank(message = "公司名称不能为空")   // 来自 javax.validation.constraints.NotBlank
private String companyName;

@Email(message = "邮箱格式不正确")       // 来自 javax.validation.constraints.Email
private String email;
```

| 注解 | 完整类名 | 校验逻辑 | 方法（Hibernate Validator 实现） |
|------|---------|---------|-------------------------------|
| `@NotBlank` | `javax.validation.constraints.NotBlank` | 不能为 null，trim 后长度 > 0 | `CharSequence.toString().trim().length() > 0` |
| `@Email` | `javax.validation.constraints.Email` | 必须包含 `@` 且域名合法 | 正则匹配 RFC 5322 邮箱格式 |

**第三层 — 业务逻辑 + MyBatis-Plus 插入**

```java
// CustomerServiceImpl.create()
if (StringUtils.isBlank(customer.getStatus())) {   // ①
    customer.setStatus("潜在客户");
}
customerMapper.insert(customer);                   // ②
```

| 步骤 | 完整方法签名 | 来自 |
|------|------------|------|
| ① | `boolean StringUtils.isBlank(CharSequence cs)` | `com.baomidou.mybatisplus.core.toolkit.StringUtils` (MyBatis-Plus 内置) |
| ② | `int BaseMapper<T>.insert(T entity)` | `com.baomidou.mybatisplus.core.mapper.BaseMapper` (MyBatis-Plus) |

**① `StringUtils.isBlank()` 详解**：

```java
// MyBatis-Plus 源码（简化）：
public static boolean isBlank(final CharSequence cs) {
    int strLen;
    if (cs == null || (strLen = cs.length()) == 0) {
        return true;   // null 或空字符串 → true
    }
    for (int i = 0; i < strLen; i++) {
        if (!Character.isWhitespace(cs.charAt(i))) {
            return false;  // 有一个非空白字符 → false
        }
    }
    return true;  // 全是空白字符 → true
}
```

> 本质上就是 JDK 的 `Character.isWhitespace(char)` + `CharSequence.length()` + `charAt()` 的组合判断。

**② `BaseMapper.insert()` 详解**：

```
BaseMapper.insert(T entity)
    │
    ├─ 1. 读取 @TableName("customer") → 确定表名
    ├─ 2. 读取 @TableId(type = IdType.AUTO) → 主键策略是数据库自增
    ├─ 3. 读取 @TableField(fill = FieldFill.INSERT) → createdAt 自动填充当前时间
    ├─ 4. 动态生成 SQL：
    │      INSERT INTO customer (company_name, contact_person, ..., created_at)
    │      VALUES (?, ?, ..., ?)
    ├─ 5. 通过 JDBC PreparedStatement 执行
    ├─ 6. 数据库返回自增主键 → 回填到 entity.id
    └─ 7. 返回受影响行数 (int)
```

| 底层步骤 | 所用 API | 来自 |
|---------|---------|------|
| 读取 `@TableName` | Java 反射 `Class.getAnnotation(TableName.class)` | JDK `java.lang.reflect` |
| 构建 SQL | MyBatis-Plus `SqlHelper` 内部拼接 | `mybatis-plus-core` |
| 执行 SQL | MyBatis `SqlSession.insert()` → JDBC `PreparedStatement.executeUpdate()` | `mybatis` → JDK `java.sql` |
| 回填主键 | JDBC `Statement.getGeneratedKeys()` | JDK `java.sql.Statement` |
| 自动填充 `createdAt` | MyBatis-Plus `MetaObjectHandler.insertFill()` | `mybatis-plus-core` |

**第四层 — 返回统一响应**

```java
return Result.ok("客户录入成功", saved);
```

调用链已在上文 2.3 节详细拆解。

#### 3.1.3 假如校验失败……

```
@Valid 校验失败
    │
    ▼
MethodArgumentNotValidException 被抛出
    │
    ▼
GlobalExceptionHandler.handleValidException() 捕获
    │
    ├─ e.getBindingResult()          ← org.springframework.validation.Errors
    │     .getFieldErrors()          ← List<FieldError>
    │     .stream()                  ← JDK Collection.stream()
    │     .map(FieldError::getDefaultMessage)  ← 提取 message 字符串
    │     .collect(Collectors.joining("; "))   ← JDK Stream 终端操作
    │
    └─ return Result.fail(400, msg)
```

| 方法 | 完整签名 | 来自 |
|------|---------|------|
| `getBindingResult()` | `BindingResult Errors.getBindingResult()` | `spring-web` |
| `getFieldErrors()` | `List<FieldError> AbstractBindingResult.getFieldErrors()` | `spring-web` |
| `FieldError.getDefaultMessage()` | `String FieldError.getDefaultMessage()` | `spring-web` — 返回 `@NotBlank(message="...")` 中的 message |
| `stream()` | `Stream<T> Collection.stream()` | JDK `java.util.Collection` |
| `map()` | `Stream<R> Stream.map(Function<T,R>)` | JDK `java.util.stream.Stream` |
| `collect()` | `R Stream.collect(Collector)` | JDK `java.util.stream.Stream` |
| `Collectors.joining("; ")` | `Collector Collectors.joining(CharSequence delimiter)` | JDK `java.util.stream.Collectors` — 用 `"; "` 连接所有字符串 |

---

### 3.2 PUT `/api/customers/{id}` — 更新客户

#### 3.2.1 调用链路

```
HTTP PUT /api/customers/3  (JSON Body)
        │
        ▼
CustomerController.update(3, customer)
    │
    ├─ @PathVariable @NotNull Long id
    │     └─ 路径变量提取 + 非空校验
    ├─ @RequestBody Customer customer
    │     └─ JSON 反序列化
    └─ customerService.update(id, customer)
            │
            ├─ ① customerMapper.selectById(id)      ← BaseMapper
            │     └─ SQL: SELECT * FROM customer WHERE id=? AND is_deleted=0
            ├─ ② 判空 → throw RuntimeException
            ├─ ③ customer.setId(id)
            ├─ ④ customer.setCreatedAt(existing.getCreatedAt())
            ├─ ⑤ customerMapper.updateById(customer)  ← BaseMapper
            │     └─ SQL: UPDATE customer SET ... WHERE id=?
            └─ ⑥ customerMapper.selectById(id)        ← 查询最新数据返回
```

#### 3.2.2 新增 API 拆解

| 步骤 | 完整方法签名 | 来自 | 说明 |
|------|------------|------|------|
| `@PathVariable` | `org.springframework.web.bind.annotation.PathVariable` | `spring-web` | 从 URL 模板 `{id}` 中提取值，调用 `Long.valueOf()` 转为 `Long` |
| `@NotNull` | `javax.validation.constraints.NotNull` | `jakarta.validation-api` | 校验 `id` 不能为 null（不同于 `@NotBlank`，`@NotNull` 只检查 null，不检查字符串内容） |
| `selectById()` | `T BaseMapper<T>.selectById(Serializable id)` | `mybatis-plus-core` | 按主键查询单条记录，`Serializable` 是 `Long` 的父接口 |
| `updateById()` | `int BaseMapper<T>.updateById(T entity)` | `mybatis-plus-core` | 根据主键更新记录。MyBatis-Plus 内部读取 `@TableId` 识别主键字段，生成 `UPDATE ... WHERE id=?` |
| `RuntimeException` | `java.lang.RuntimeException(String message)` | JDK | 我们手动抛出，被 `GlobalExceptionHandler.handleRuntimeException()` 捕获 |

**`@PathVariable` 的底层实现**：

```java
// Spring 内部处理（简化版）：
// 1. 解析 URL 模板 "/api/customers/{id}" → 提取变量名 "id"
// 2. 匹配实际 URL "/api/customers/3" → 提取变量值 "3"
// 3. 调用类型转换器将 String "3" 转为 Long 3L：
//    org.springframework.core.convert.ConversionService.convert("3", Long.class)
// 4. 传入方法参数
```

**`BaseMapper.updateById()` 的底层行为**：

```
BaseMapper.updateById(customer)
    │
    ├─ 1. 反射读取 @TableId → 找到 id 字段，值=3
    ├─ 2. 反射读取 @TableField(fill = FieldFill.INSERT_UPDATE)
    │      → updatedAt 自动填充当前时间
    ├─ 3. 动态 SQL（只更新非 null 字段？ 否！updateById 默认全量更新）：
    │      UPDATE customer SET
    │        company_name=?, contact_person=?, ..., updated_at=?
    │      WHERE id=?
    ├─ 4. JDBC PreparedStatement 执行
    └─ 5. 返回受影响行数
```

> ⚠️ `updateById()` 默认是**全字段更新**（包括 null 值字段）。如需只更新非 null 字段，应使用 `update(null, new LambdaUpdateWrapper<Customer>().eq(...).set(...))`。

---

### 3.3 DELETE `/api/customers/{id}` — 删除客户

#### 3.3.1 核心机制：逻辑删除不是物理删除

```java
customerMapper.deleteById(id);
```

**MyBatis-Plus 逻辑删除的完整机制**：

```
我们的代码：customerMapper.deleteById(3L)

MyBatis-Plus 拦截器检测到 @TableLogic 注解在 isDeleted 字段上：
    │
    ▼
把 DELETE 语句改写为 UPDATE 语句：

实际执行的 SQL：
    UPDATE customer SET is_deleted = 1 WHERE id = 3 AND is_deleted = 0

而不是：
    DELETE FROM customer WHERE id = 3   ← 这个被拦截器阻止了
```

| 涉及的底层 API | 完整类名 | 说明 |
|---------------|---------|------|
| `@TableLogic` | `com.baomidou.mybatisplus.annotation.TableLogic` | 标记哪个字段是逻辑删除标记 |
| `LogicSqlInjector` | `com.baomidou.mybatisplus.core.injector.methods.LogicDelete` | MyBatis-Plus 内部拦截器，自动将 DELETE → UPDATE |
| `application.yml` 中的配置 | `logic-delete-value: 1` / `logic-not-delete-value: 0` | 配置删除/未删除的字段值 |

**并且，所有 SELECT 查询会自动附加 `AND is_deleted = 0`**：

```
BaseMapper.selectById(3L)
→ SQL: SELECT * FROM customer WHERE id = 3 AND is_deleted = 0

BaseMapper.selectList(wrapper)
→ SQL: SELECT * FROM customer WHERE ... AND is_deleted = 0

BaseMapper.selectPage(page, wrapper)
→ SQL: SELECT * FROM customer WHERE ... AND is_deleted = 0 LIMIT ...
```

这就是逻辑删除的核心价值：**应用层代码不需要感知"删除"这个概念，所有查询自动过滤已删除数据**。

---

### 3.4 GET `/api/customers/{id}` — 查询详情

#### 3.4.1 调用链路

```
HTTP GET /api/customers/3
        │
        ▼
CustomerController.getById(@PathVariable @NotNull Long id)
    │
    ├─ customerService.getById(id)
    │     └─ customerMapper.selectById(id)   ← BaseMapper
    ├─ if (customer == null)
    │     └─ Result.fail(404, "客户不存在")
    └─ return Result.ok(customer)
```

#### 3.4.2 Jackson 序列化过程（JSON输出关键）

返回 `Result.ok(customer)` 后，Spring MVC 调用 Jackson 将 Java 对象转为 JSON 字符串：

```
Result<Customer> 对象
    │
    ▼
com.fasterxml.jackson.databind.ObjectMapper.writeValueAsString(result)
    │
    ├─ 读取 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    │     → LocalDateTime 字段按指定格式序列化
    ├─ 读取 application.yml: jackson.default-property-inclusion: non_null
    │     → null 值字段不出现在 JSON 中
    └─ 输出 JSON 字符串
```

| Jackson 核心类/方法 | 完整类名 | 说明 |
|-------------------|---------|------|
| `ObjectMapper` | `com.fasterxml.jackson.databind.ObjectMapper` | Jackson 核心类，所有序列化/反序列化入口 |
| `writeValueAsString()` | `String ObjectMapper.writeValueAsString(Object)` | Java 对象 → JSON 字符串 |
| `@JsonFormat` | `com.fasterxml.jackson.annotation.JsonFormat` | 控制日期序列化格式 |
| `non_null` 配置 | `spring.jackson.default-property-inclusion` | 全局配置：null 字段不输出 |

---

### 3.5 POST `/api/customers/list` — 分页条件查询

这是**最复杂**的接口，涉及 MyBatis-Plus 的 `LambdaQueryWrapper`、`Page` 和排序构建。

#### 3.5.1 调用链路全景图

```
POST /api/customers/list  (JSON: {pageNum:1, pageSize:10, keyword:"深圳"})
        │
        ▼
CustomerController.list(CustomerQueryDTO queryDTO)
    │
    ├─ customerService.listByCondition(queryDTO)
    └─ 手动组装 Map<String, Object> 返回
            │
            ▼
CustomerServiceImpl.listByCondition(dto)
    │
    ├─ ① new Page<>(dto.getPageNum(), dto.getPageSize())
    ├─ ② LambdaQueryWrapper<Customer> wrapper = buildQueryWrapper(dto)
    └─ ③ customerMapper.selectPage(page, wrapper)
            │
            ▼
buildQueryWrapper(dto)  — 核心：动态查询条件构建
    │
    ├─ new LambdaQueryWrapper<>()
    ├─ wrapper.eq(Customer::getCustomerType, dto.getCustomerType())
    ├─ wrapper.like(Customer::getCompanyName, dto.getKeyword())
    ├─ wrapper.and(...)   ← 嵌套条件组
    ├─ wrapper.or()       ← OR 逻辑
    └─ wrapper.orderBy(true, isAsc, Customer::getCreatedAt)
```

#### 3.5.2 `LambdaQueryWrapper` 核心方法逐一拆解

这是 MyBatis-Plus 最精巧的设计：**用 Lambda 表达式代替字符串字段名**，编译期就能检查字段名是否正确。

**(A) 创建分页对象**

```java
Page<Customer> page = new Page<>(dto.getPageNum(), dto.getPageSize());
```

| 构造方法 | 完整签名 | 来源 |
|---------|---------|------|
| `new Page<>(long, long)` | `Page<T>(long current, long size)` | `com.baomidou.mybatisplus.extension.plugins.pagination.Page` |

| 参数 | 含义 | 从哪来 |
|------|------|--------|
| `current` | 当前页码（从1开始） | `dto.getPageNum()` → 默认 1 |
| `size` | 每页条数 | `dto.getPageSize()` → 默认 10 |

> **分页插件介入**：`MyBatisPlusConfig` 注册的 `PaginationInnerInterceptor` 拦截 `selectPage()` 调用，读取 `Page` 对象的 current/size 字段，在生成的 SQL 末尾自动拼接 `LIMIT offset, size`。

**(B) 创建查询包装器**

```java
LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
```

| 构造器 | 完整签名 | 泛型含义 |
|--------|---------|---------|
| `new LambdaQueryWrapper<>()` | `LambdaQueryWrapper<T>()` | `T = Customer` → 后续的 Lambda 表达式都基于 `Customer` 的方法引用 |

**(C) 等值匹配 — `eq()`**

```java
wrapper.eq(Customer::getCustomerType, dto.getCustomerType());
```

| 方法 | 完整签名 | 参数解释 |
|------|---------|---------|
| `eq()` | `LambdaQueryWrapper<T> eq(SFunction<T,?> column, Object val)` | ① `SFunction<T,?>` 是 MyBatis-Plus 定义的函数式接口，通过 `Customer::getCustomerType` 方法引用 → 反射解析出字段名 `customer_type` ② `Object val` 是要匹配的值 → 生成 `AND customer_type = ?` |

**方法引用 → 字段名的转换过程**：

```
Customer::getCustomerType
    │
    ▼ (MyBatis-Plus 内部)
LambdaMetaExtractor 解析这个 Lambda：
    ├─ 识别出它对应 Customer 类的 getCustomerType() 方法
    ├─ 去掉 "get" 前缀 → "CustomerType"
    ├─ 应用驼峰→下划线规则 → "customer_type"
    └─ 拼接到 SQL: AND customer_type = ?
```

> 这正是 `LambdaQueryWrapper` 相比字符串 `"customer_type"` 的优势：字段名改了，编译器会报错，运行时不会悄无声息地出错。

**(D) 模糊匹配 — `like()`**

```java
wrapper.like(Customer::getCompanyName, dto.getKeyword());
```

| 方法 | 完整签名 | SQL 生成 |
|------|---------|---------|
| `like()` | `LambdaQueryWrapper<T> like(SFunction<T,?> column, Object val)` | `AND company_name LIKE '%深圳%'` |

**(E) 嵌套条件组 — `and()` + `or()`**

```java
wrapper.and(w -> w
    .like(Customer::getCompanyName, dto.getKeyword())
    .or()
    .like(Customer::getContactPerson, dto.getKeyword())
);
```

| 方法 | 完整签名 | 作用 |
|------|---------|------|
| `and()` | `LambdaQueryWrapper<T> and(Consumer<LambdaQueryWrapper<T>> consumer)` | 创建一个括号包裹的子条件组。Lambda 表达式 `w -> w...` 内部的 `w` 是一个**全新的子 Wrapper** |
| `or()` | `LambdaQueryWrapper<T> or()` | 将下一个条件与前一个条件用 `OR` 连接（默认是 `AND`） |

生成的 SQL：

```sql
AND (
    company_name LIKE '%深圳%'
    OR
    contact_person LIKE '%深圳%'
)
```

**(F) 排序 — `orderBy()`**

```java
wrapper.orderBy(true, isAsc, Customer::getCreatedAt);
```

| 方法 | 完整签名 | 参数说明 |
|------|---------|---------|
| `orderBy()` | `LambdaQueryWrapper<T> orderBy(boolean condition, boolean isAsc, SFunction<T,?> column)` | ① `condition`：是否应用此排序（我们传 `true`，始终应用）② `isAsc`：true=升序，false=降序 ③ `column`：排序字段的方法引用 |

生成的 SQL：`ORDER BY created_at DESC`

**(G) 执行分页查询**

```java
customerMapper.selectPage(page, wrapper);
```

| 方法 | 完整签名 | 来源 |
|------|---------|------|
| `selectPage()` | `P page BaseMapper<T>.selectPage(P page, Wrapper<T> queryWrapper)` | `com.baomidou.mybatisplus.core.mapper.BaseMapper` |

**内部执行步骤**：

```
BaseMapper.selectPage(page, wrapper)
    │
    ├─ 1. MyBatis-Plus 分页拦截器 (PaginationInnerInterceptor) 接入
    │       ├─ 先执行 COUNT 查询：SELECT COUNT(*) FROM customer WHERE ... AND is_deleted=0
    │       │   → 得到 total = 100
    │       │   → 写入 page.total 字段
    │       ├─ 计算 offset = (pageNum - 1) * pageSize
    │       ├─ 拼接 LIMIT offset, pageSize
    │       └─ 执行 SELECT * FROM customer WHERE ... AND is_deleted=0 ORDER BY created_at DESC LIMIT 0, 10
    │
    ├─ 2. 查询结果 → List<Customer> → 写入 page.records
    └─ 3. 返回 Page 对象（包含了 total, records, current, size, pages）
```

| `Page` 的返回值方法 | 完整签名 | 含义 |
|-------------------|---------|------|
| `getRecords()` | `List<T> Page.getRecords()` | 当前页数据 |
| `getTotal()` | `long Page.getTotal()` | 总记录数 |
| `getCurrent()` | `long Page.getCurrent()` | 当前页码 |
| `getSize()` | `long Page.getSize()` | 每页条数 |
| `getPages()` | `long Page.getPages()` | 总页数 (total/size 向上取整) |

**(H) Controller 层组装 HashMap 返回**

```java
Map<String, Object> result = new HashMap<>();
result.put("records", page.getRecords());
result.put("total", page.getTotal());
// ...
return Result.ok(result);
```

| JDK 方法 | 完整签名 | 说明 |
|---------|---------|------|
| `new HashMap<>()` | `HashMap()` 构造器 | JDK `java.util.HashMap` — 无序键值对容器 |
| `put()` | `V Map.put(K key, V value)` | JDK `java.util.Map` — 存入键值对 |

---

### 3.6 GET `/api/customers/export` — Excel 导出

这是**涉及技术栈最多**的接口：Spring MVC → MyBatis-Plus → Apache POI → Servlet API。

#### 3.6.1 调用链路全景图

```
GET /api/customers/export?keyword=深圳&status=已签约
        │
        ▼
CustomerController.export(@RequestParam ...)
    │
    ├─ ① 收集 @RequestParam → 组装 CustomerQueryDTO
    ├─ ② dto.setPageSize(Integer.MAX_VALUE)  ← 不设分页
    └─ ③ customerService.exportExcel(dto, response)
            │
            ├─ ④ buildQueryWrapper(dto)       ← 同 3.5 节
            ├─ ⑤ customerMapper.selectList(wrapper)  ← BaseMapper 全量查询
            └─ ⑥ ExcelUtil.export(response, list, "客户资料")
                    │
                    ├─ Ⓐ new XSSFWorkbook()           ← Apache POI
                    ├─ Ⓑ workbook.createSheet("客户资料")
                    ├─ Ⓒ sheet.createRow(0)            ← 表头行
                    ├─ Ⓓ 逐行填充数据
                    ├─ Ⓔ 设置列宽
                    ├─ Ⓕ sheet.createFreezePane(0,1)   ← 冻结首行
                    ├─ Ⓖ response.setContentType(...)  ← Servlet API
                    ├─ Ⓗ URLEncoder.encode(...)        ← JDK
                    └─ Ⓘ workbook.write(os)            ← 输出文件流
```

#### 3.6.2 ExcelUtil 中每一个 Apache POI 方法的拆解

**(A) 创建工作簿**

```java
Workbook workbook = new XSSFWorkbook();
```

| 元素 | 完整类名/说明 |
|------|------------|
| `Workbook` | `org.apache.poi.ss.usermodel.Workbook` — POI 的**接口**，代表一个 Excel 文件 |
| `XSSFWorkbook` | `org.apache.poi.xssf.usermodel.XSSFWorkbook` — `.xlsx` 格式的实现类（基于 XML） |
| 构造器 | `XSSFWorkbook()` — 在内存中创建一个空的 .xlsx 工作簿 |

> POI 还有 `HSSFWorkbook`（`.xls` 格式，基于二进制），我们用 `XSSF` 因其支持更大行列数（1048576 行 × 16384 列）和更好的样式。

**(B) 创建工作表**

```java
Sheet sheet = workbook.createSheet("客户资料");
```

| 方法 | 完整签名 | 说明 |
|------|---------|------|
| `createSheet()` | `Sheet Workbook.createSheet(String sheetname)` | 创建名为"客户资料"的工作表，返回 `Sheet` 接口。如果同名已存在会抛异常（我们新建的工作簿不会有冲突）。 |

**(C) 创建表头行**

```java
Row headerRow = sheet.createRow(0);
CellStyle headerStyle = createHeaderStyle(workbook);
for (int i = 0; i < HEADERS.length; i++) {
    Cell cell = headerRow.createCell(i);
    cell.setCellValue(HEADERS[i]);
    cell.setCellStyle(headerStyle);
}
```

| 方法 | 完整签名 | 说明 |
|------|---------|------|
| `createRow(0)` | `Row Sheet.createRow(int rownum)` | 在第 0 行创建行（Excel 行号从 0 开始）。如果该位置已有行，覆盖之。 |
| `createCell(i)` | `Cell Row.createCell(int column)` | 在第 i 列创建单元格 |
| `setCellValue(String)` | `void Cell.setCellValue(String value)` | 设置单元格的文本值 |
| `setCellStyle(CellStyle)` | `void Cell.setCellStyle(CellStyle style)` | 给单元格应用样式 |

**(D) 创建表头样式 — 字体操作**

```java
CellStyle style = workbook.createCellStyle();

Font font = workbook.createFont();          // ① 创建字体对象
font.setBold(true);                         // ② 加粗
font.setFontHeightInPoints((short) 12);      // ③ 字号12磅
font.setColor(IndexedColors.WHITE.getIndex());// ④ 白色字体
style.setFont(font);                         // ⑤ 将字体应用到样式
```

| 方法 | 完整签名 | 来自 | 说明 |
|------|---------|------|------|
| ① `createFont()` | `Font Workbook.createFont()` | POI `Workbook` | 创建一个可配置的字体对象 |
| ② `setBold(true)` | `void Font.setBold(boolean bold)` | POI `Font` | 字体是否加粗 |
| ③ `setFontHeightInPoints(12)` | `void Font.setFontHeightInPoints(short height)` | POI `Font` | 字体磅值（12 = 小四号） |
| ④ `setColor(...)` | `void Font.setColor(short color)` | POI `Font` | 字体颜色，`IndexedColors.WHITE.getIndex()` 返回预定义的白色索引值 |
| ⑤ `setFont(font)` | `void CellStyle.setFont(Font font)` | POI `CellStyle` | 将字体对象绑定到样式 |

**(E) 背景色与对齐**

```java
style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());  // ⑥ 前景色=深蓝
style.setFillPattern(FillPatternType.SOLID_FOREGROUND);             // ⑦ 实心填充
style.setAlignment(HorizontalAlignment.CENTER);                     // ⑧ 水平居中
style.setVerticalAlignment(VerticalAlignment.CENTER);               // ⑨ 垂直居中
```

| 方法 | 完整签名 | 说明 |
|------|---------|------|
| ⑥ `setFillForegroundColor()` | `void CellStyle.setFillForegroundColor(short color)` | 设置填充前景色 |
| ⑦ `setFillPattern()` | `void CellStyle.setFillPattern(FillPatternType fp)` | 必须设为 `SOLID_FOREGROUND`，否则前景色不显示。这是 POI 的常见坑 |
| ⑧ `setAlignment()` | `void CellStyle.setAlignment(HorizontalAlignment align)` | 水平对齐方式，`CENTER` = 居中 |
| ⑨ `setVerticalAlignment()` | `void CellStyle.setVerticalAlignment(VerticalAlignment align)` | 垂直对齐 |

**(F) 边框样式**

```java
style.setBorderBottom(BorderStyle.THIN);   // 下边框：细线
style.setBorderTop(BorderStyle.THIN);      // 上边框
style.setBorderLeft(BorderStyle.THIN);     // 左边框
style.setBorderRight(BorderStyle.THIN);    // 右边框
```

| 方法 | 完整签名 | `BorderStyle.THIN` 的含义 |
|------|---------|--------------------------|
| `setBorderXxx()` | `void CellStyle.setBorderXxx(BorderStyle border)` | `THIN` 是 POI 预定义的枚举值，表示最细的线型 |

**(G) 设置列宽**

```java
sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
```

| 方法 | 完整签名 | 参数说明 |
|------|---------|---------|
| `setColumnWidth()` | `void Sheet.setColumnWidth(int columnIndex, int width)` | `columnIndex` = 列号（0开始，A列=0）`width` = 列宽，**单位是 1/256 个字符宽度**。所以 `25 * 256` = 25 个字符宽 |

> **为什么乘 256？** 这是 POI 的历史设计决策。一个"字符宽度" = 256 个单位。你想设 25 个字符宽，就得写 `25 * 256 = 6400`。

**(H) 冻结首行**

```java
sheet.createFreezePane(0, 1);
```

| 方法 | 完整签名 | 参数说明 |
|------|---------|---------|
| `createFreezePane()` | `Sheet createFreezePane(int colSplit, int rowSplit)` | `colSplit=0`：不冻结列 `rowSplit=1`：冻结第1行（即第0行表头在滚动时保持可见） |

**(I) HTTP 响应头设置（Servlet API）**

```java
response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
response.setCharacterEncoding("UTF-8");
response.setHeader("Content-Disposition",
    "attachment; filename*=UTF-8''" + encodedFileName);
```

| 方法 | 完整签名 | 来自 | 说明 |
|------|---------|------|------|
| `setContentType()` | `void ServletResponse.setContentType(String type)` | `javax.servlet.ServletResponse` | 告诉浏览器：返回的是 .xlsx 文件，不是 HTML |
| `setCharacterEncoding()` | `void ServletResponse.setCharacterEncoding(String charset)` | `javax.servlet.ServletResponse` | 设置编码为 UTF-8 |
| `setHeader()` | `void HttpServletResponse.setHeader(String name, String value)` | `javax.servlet.http.HttpServletResponse` | 设置任意 HTTP 响应头。`Content-Disposition: attachment` 触发浏览器下载行为 |

**MIME Type 解释**：`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` 是 `.xlsx` 文件的标准 MIME 类型。浏览器识别这个类型后，会：
- 不尝试在页面中显示内容
- 配合 `Content-Disposition: attachment` 触发"另存为"对话框

**(J) 文件名编码（JDK）**

```java
String encodedFileName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8.toString())
        .replaceAll("\\+", "%20");
```

| 方法 | 完整签名 | 来自 | 说明 |
|------|---------|------|------|
| `URLEncoder.encode()` | `String URLEncoder.encode(String s, String enc)` | JDK `java.net.URLEncoder` | 将中文文件名编码为 `%E5%AE%A2%E6%88%B7...` 格式。`enc="UTF-8"` 指定字符集 |
| `StandardCharsets.UTF_8.toString()` | `String Charset.toString()` | JDK `java.nio.charset.StandardCharsets` | 返回 `"UTF-8"` 字符串。等价于直接写 `"UTF-8"`，但更安全（编译期检查） |
| `.replaceAll("\\+", "%20")` | `String String.replaceAll(String regex, String replacement)` | JDK `java.lang.String` | `URLEncoder` 会把空格编码为 `+`，但 `Content-Disposition` 头中空格应编码为 `%20`。这是一个兼容性细节 |

**(K) 输出文件流**

```java
try (OutputStream os = response.getOutputStream()) {
    workbook.write(os);
    os.flush();
}
```

| 方法 | 完整签名 | 来自 | 说明 |
|------|---------|------|------|
| `getOutputStream()` | `ServletOutputStream ServletResponse.getOutputStream()` | `javax.servlet.ServletResponse` | 获取响应输出流，数据通过此流发送到浏览器 |
| `workbook.write(os)` | `void Workbook.write(OutputStream stream)` | POI `Workbook` | 将整个工作簿写入输出流（生成 .xlsx 文件的字节数据） |
| `os.flush()` | `void OutputStream.flush()` | JDK `java.io.OutputStream` | 强制将缓冲区中的数据刷出，确保完整发送 |
| `try (...)` 语法 | JDK try-with-resources | JDK 7+ | 自动在块结束时调用 `os.close()`，释放资源 |

> POI 的 `write()` 方法内部会把所有 Sheet、Row、Cell 序列化为 Office Open XML 格式（本质是一个 ZIP 包内含多个 XML 文件），写入 OutputStream。

**(L) `DateTimeFormatter` 日期格式化**

```java
// 静态字段
private static final DateTimeFormatter DATE_FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

// 使用时：
private static String formatDate(LocalDateTime dt) {
    return dt == null ? "" : dt.format(DATE_FMT);
}
```

| 方法 | 完整签名 | 来自 | 说明 |
|------|---------|------|------|
| `DateTimeFormatter.ofPattern()` | `DateTimeFormatter DateTimeFormatter.ofPattern(String pattern)` | JDK `java.time.format.DateTimeFormatter` | 创建日期格式化器。`"yyyy-MM-dd HH:mm:ss"` → 如 `2026-06-30 14:30:00` |
| `LocalDateTime.format()` | `String LocalDateTime.format(DateTimeFormatter formatter)` | JDK `java.time.LocalDateTime` | 将日期时间对象按指定格式转为字符串 |

---

### 3.7 POST `/api/customers/share` — 共享浏览

该接口与 3.5 节（分页查询）高度相似，唯一区别是多了一行：

```java
wrapper.eq(Customer::getStatus, "已签约");
```

这行代码强制筛选 `status = '已签约'`，保证外部用户只能看到已签约客户。所用 API 已在 3.5.2(C) 节详细拆解，不再重复。

**额外差异点**：

```java
// 共享浏览不返回 pages 字段
result.put("records", page.getRecords());
result.put("total", page.getTotal());
result.put("pageNum", page.getCurrent());
result.put("pageSize", page.getSize());
// 注意：没有 result.put("pages", page.getPages());
```

这减少了对外的信息暴露（不告诉外部一共有多少页）。

---

### 3.8 GET `/api/customers/types` — 枚举查询

这是最简洁的接口 — 返回 Java 内存中的静态常量列表。

```java
// Service 层
private static final List<String> CUSTOMER_TYPES =
    Arrays.asList("店铺购买", "开店培训", "合作工厂", "物流合作", "其他");

// Controller 层
return Result.ok(customerService.getCustomerTypes());
```

| 方法 | 完整签名 | 来自 | 说明 |
|------|---------|------|------|
| `Arrays.asList()` | `List<T> java.util.Arrays.asList(T... a)` | JDK `java.util.Arrays` | 将可变参数转为**固定大小的 List**（不能 add/remove，但可以 get/set）。返回 `Arrays$ArrayList` 内部类 |

> `Arrays.asList()` 返回的 List 是 Arrays 的内部类，**不支持** `add()` 和 `remove()`。这里只读使用，完全没问题。

---

## 4. 横切关注点拆解

这些 API 不属于任何一个具体接口，而是**对所有接口生效**。

### 4.1 跨域 CORS — `CorsConfig`

```java
@Configuration          // ① 声明这是配置类
public class CorsConfig {
    @Bean               // ② 声明返回值是一个 Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");     // ③
        config.setAllowCredentials(true);        // ④
        config.addAllowedMethod("*");            // ⑤
        config.addAllowedHeader("*");            // ⑥
        config.setMaxAge(3600L);                 // ⑦

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // ⑧
        return new CorsFilter(source);                    // ⑨
    }
}
```

| 步骤 | 完整类名 | 来自 jar | 说明 |
|------|---------|---------|------|
| ① `@Configuration` | `org.springframework.context.annotation.Configuration` | `spring-context` | 标记此类为 Spring 配置类，启动时会扫描并处理 |
| ② `@Bean` | `org.springframework.context.annotation.Bean` | `spring-context` | 方法返回值由 Spring 容器管理，默认单例 |
| ③ `addAllowedOriginPattern("*")` | `CorsConfiguration.addAllowedOriginPattern(String)` | `spring-web` | 允许的跨域来源，`*` = 所有域名 |
| ④ `setAllowCredentials(true)` | `CorsConfiguration.setAllowCredentials(Boolean)` | `spring-web` | 是否允许携带 Cookie |
| ⑤ `addAllowedMethod("*")` | `CorsConfiguration.addAllowedMethod(String)` | `spring-web` | 允许的 HTTP 方法，`*` = 所有 |
| ⑥ `addAllowedHeader("*")` | `CorsConfiguration.addAllowedHeader(String)` | `spring-web` | 允许的请求头 |
| ⑦ `setMaxAge(3600L)` | `CorsConfiguration.setMaxAge(Long)` | `spring-web` | 预检请求的缓存时间（秒） |
| ⑧ `registerCorsConfiguration("/**", config)` | `UrlBasedCorsConfigurationSource.registerCorsConfiguration(String, CorsConfiguration)` | `spring-web` | 对所有URL路径应用此CORS策略 |
| ⑨ `new CorsFilter(source)` | `org.springframework.web.filter.CorsFilter` | `spring-web` | 创建一个 Servlet Filter，在请求进入 Controller 前添加 CORS 响应头 |

**CORS 的工作原理**：

```
浏览器发起跨域请求
    │
    ▼
浏览器先发送 OPTIONS 预检请求（Preflight）
    │
    ▼
CorsFilter 拦截 → 检查配置 → 返回允许的 CORS 头：
    Access-Control-Allow-Origin: *
    Access-Control-Allow-Methods: GET,POST,PUT,DELETE
    Access-Control-Allow-Headers: *
    Access-Control-Max-Age: 3600
    │
    ▼
浏览器收到预检响应 → 确认允许 → 发送真正的请求
```

### 4.2 全局异常处理 — `GlobalExceptionHandler`

核心注解和方法已在上文 3.1.3 节结合参数校验流程拆解。这里补充 **Spring 异常处理机制**：

| 注解/类 | 完整类名 | 来自 | 说明 |
|---------|---------|------|------|
| `@RestControllerAdvice` | `org.springframework.web.bind.annotation.RestControllerAdvice` | `spring-web` | 组合了 `@ControllerAdvice` + `@ResponseBody`。Spring 启动时扫描此注解，将类注册为全局异常处理器 |
| `@ExceptionHandler(Xxx.class)` | `org.springframework.web.bind.annotation.ExceptionHandler` | `spring-web` | 声明该方法处理哪种异常类型。当一个 Controller 抛出异常时，Spring 按类型匹配最近的 `@ExceptionHandler` 方法 |
| `@ResponseStatus(HttpStatus.BAD_REQUEST)` | `org.springframework.web.bind.annotation.ResponseStatus` | `spring-web` | 设置 HTTP 响应状态码为 400 |

**异常匹配优先级**：

```
Controller 抛出异常
    │
    ▼
Spring 查找 @ExceptionHandler：
    1. 先精确匹配异常类型
    2. 再匹配父类型
    3. 最后匹配 Exception.class（兜底）

实际优先级：
    MethodArgumentNotValidException  → handleValidException()      ← 精确
    BindException                    → handleBindException()       ← 精确
    ConstraintViolationException     → handleConstraintViolation() ← 精确
    RuntimeException                 → handleRuntimeException()    ← 父类型
    Exception                        → handleException()           ← 兜底
```

### 4.3 MyBatis-Plus 分页插件 — `MyBatisPlusConfig`

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
    pagination.setMaxLimit(500L);
    pagination.setOverflow(true);
    interceptor.addInnerInterceptor(pagination);
    return interceptor;
}
```

| 类/方法 | 完整签名 | 说明 |
|---------|---------|------|
| `MybatisPlusInterceptor` | `com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor` | MyBatis-Plus 拦截器链容器，管理所有内部拦截器 |
| `PaginationInnerInterceptor(DbType.MYSQL)` | 构造器参数为数据库方言枚举 | 告诉分页插件当前是 MySQL，以便生成正确的 LIMIT 语法 |
| `setMaxLimit(500L)` | `void PaginationInnerInterceptor.setMaxLimit(long maxLimit)` | 单页最大条数。如果前端传 `pageSize=99999`，实际只查 500 条 |
| `setOverflow(true)` | `void PaginationInnerInterceptor.setOverflow(boolean overflow)` | 页码溢出处理：请求第 100 页但总共只有 3 页 → 自动返回第 1 页 |

---

## 5. 底层API速查索引

以下按"你引入的 jar 包 → 你实际调用的类/方法"组织。

### 5.1 JDK 标准库 (java.*)

| 类 | 调用的方法 | 使用位置 | 作用 |
|----|----------|---------|------|
| `java.lang.System` | `currentTimeMillis()` | `Result.java` | 生成时间戳 |
| `java.util.HashMap` | `new HashMap<>()`, `put()` | `CustomerController.list()`, `shareList()` | 组装分页响应 |
| `java.util.Arrays` | `asList(T...)` | `CustomerServiceImpl` | 创建客户类型/状态枚举列表 |
| `java.util.List` | `size()`, `get()`, `stream()` 等 | 多处 | 集合操作 |
| `java.util.stream.Stream` | `map()`, `collect()` | `GlobalExceptionHandler` | 函数式处理异常信息 |
| `java.util.stream.Collectors` | `joining("; ")` | `GlobalExceptionHandler` | 拼接校验错误信息 |
| `java.time.LocalDateTime` | `format(DateTimeFormatter)` | `ExcelUtil` | 日期转字符串 |
| `java.time.format.DateTimeFormatter` | `ofPattern("yyyy-MM-dd HH:mm:ss")` | `ExcelUtil` | 定义日期格式 |
| `java.net.URLEncoder` | `encode(String, String)` | `ExcelUtil` | URL编码中文文件名 |
| `java.nio.charset.StandardCharsets` | `UTF_8.toString()` | `ExcelUtil` | 获取"UTF-8"字符串 |
| `java.io.OutputStream` | `flush()`, `close()` | `ExcelUtil` | 输出文件流 |
| `java.lang.String` | `replaceAll()`, `equalsIgnoreCase()` | `ExcelUtil`, `CustomerServiceImpl` | 字符串处理 |
| `java.lang.RuntimeException` | `new RuntimeException(msg)` | `CustomerServiceImpl` | 抛出业务异常 |
| `java.lang.Integer` | `MAX_VALUE` | `CustomerController.export()` | 导出时不设分页上限 |

### 5.2 Servlet API (javax.servlet)

| 类 | 调用的方法 | 使用位置 |
|----|----------|---------|
| `javax.servlet.http.HttpServletResponse` | `setContentType()`, `setCharacterEncoding()`, `setHeader()`, `getOutputStream()` | `ExcelUtil` |

### 5.3 Spring Framework (org.springframework)

| 类/注解 | 作用 | 使用位置 |
|---------|------|---------|
| `@SpringBootApplication` | 标记启动类 | `CrmApplication` |
| `@RestController` | 声明 REST 控制器 | `CustomerController` |
| `@RequestMapping("/api/customers")` | URL 前缀映射 | `CustomerController` |
| `@PostMapping` / `@GetMapping` / `@PutMapping` / `@DeleteMapping` | HTTP 方法 + URL 映射 | `CustomerController` |
| `@RequestBody` | JSON 反序列化入参 | `CustomerController` |
| `@PathVariable` | 提取 URL 路径变量 | `CustomerController` |
| `@RequestParam` | 提取 URL Query 参数 | `CustomerController` |
| `@Service` | 声明 Service Bean | `CustomerServiceImpl` |
| `@Configuration` | 声明配置类 | `CorsConfig`, `MyBatisPlusConfig` |
| `@Bean` | 声明 Bean 工厂方法 | `CorsConfig`, `MyBatisPlusConfig` |
| `@RestControllerAdvice` | 声明全局异常处理器 | `GlobalExceptionHandler` |
| `@ExceptionHandler` | 声明异常处理方法 | `GlobalExceptionHandler` |
| `@ResponseStatus` | 设置 HTTP 响应状态码 | `GlobalExceptionHandler` |
| `CorsConfiguration` | CORS 策略对象 | `CorsConfig` |
| `CorsFilter` | CORS 过滤器 | `CorsConfig` |
| `UrlBasedCorsConfigurationSource` | URL-CORS 映射注册表 | `CorsConfig` |
| `MethodArgumentNotValidException` | @Valid 校验失败的异常类型 | `GlobalExceptionHandler` |
| `FieldError` | 字段级校验错误对象 | `GlobalExceptionHandler` |

### 5.4 Spring Boot Validation (javax.validation / jakarta.validation)

| 注解 | 完整类名 | 使用位置 |
|------|---------|---------|
| `@Valid` | `javax.validation.Valid` | `CustomerController.create()` |
| `@Validated` | `org.springframework.validation.annotation.Validated` | `CustomerController` 类上 |
| `@NotNull` | `javax.validation.constraints.NotNull` | `CustomerController` 路径参数 |
| `@NotBlank` | `javax.validation.constraints.NotBlank` | `Customer` 实体字段 |
| `@Email` | `javax.validation.constraints.Email` | `Customer` 实体字段 |

### 5.5 MyBatis-Plus (com.baomidou.mybatisplus)

| 类 | 调用的方法 | 使用位置 |
|----|----------|---------|
| `BaseMapper<T>` | `insert(T)` | `CustomerServiceImpl.create()` |
| `BaseMapper<T>` | `updateById(T)` | `CustomerServiceImpl.update()` |
| `BaseMapper<T>` | `deleteById(Serializable)` | `CustomerServiceImpl.delete()` |
| `BaseMapper<T>` | `selectById(Serializable)` | `CustomerServiceImpl.getById()`, `update()` |
| `BaseMapper<T>` | `selectPage(Page<T>, Wrapper<T>)` | `CustomerServiceImpl.listByCondition()` |
| `BaseMapper<T>` | `selectList(Wrapper<T>)` | `CustomerServiceImpl.exportExcel()` |
| `LambdaQueryWrapper<T>` | 构造器 `new LambdaQueryWrapper<>()` | `CustomerServiceImpl.buildQueryWrapper()` |
| `LambdaQueryWrapper<T>` | `eq(SFunction, Object)` | 同上 |
| `LambdaQueryWrapper<T>` | `like(SFunction, Object)` | 同上 |
| `LambdaQueryWrapper<T>` | `and(Consumer)` | 同上 |
| `LambdaQueryWrapper<T>` | `or()` | 同上 |
| `LambdaQueryWrapper<T>` | `orderBy(boolean, boolean, SFunction)` | `CustomerServiceImpl.applySorting()` |
| `Page<T>` | `new Page<>(long, long)` | `CustomerServiceImpl.listByCondition()` |
| `Page<T>` | `getRecords()`, `getTotal()`, `getCurrent()`, `getSize()`, `getPages()` | `CustomerController` |
| `StringUtils` | `isBlank(CharSequence)` | `CustomerServiceImpl.create()` |
| `StringUtils` | `isNotBlank(CharSequence)` | `CustomerServiceImpl.buildQueryWrapper()` |
| `@TableName` | 映射实体→表名 | `Customer` |
| `@TableId(type=IdType.AUTO)` | 主键自增策略 | `Customer` |
| `@TableField(fill=FieldFill.INSERT)` | 插入时自动填充 | `Customer.createdAt` |
| `@TableLogic` | 逻辑删除标记 | `Customer.isDeleted` |
| `@MapperScan` | 扫描 Mapper 接口 | `CrmApplication` |
| `MybatisPlusInterceptor` | 拦截器链容器 | `MyBatisPlusConfig` |
| `PaginationInnerInterceptor` | 分页拦截器 | `MyBatisPlusConfig` |
| `DbType.MYSQL` | MySQL 方言枚举 | `MyBatisPlusConfig` |

### 5.6 Apache POI (org.apache.poi)

| 类 | 调用的方法 | 使用位置 |
|----|----------|---------|
| `XSSFWorkbook` | 构造器 `new XSSFWorkbook()` | `ExcelUtil.export()` |
| `Workbook` | `createSheet(String)`, `createFont()`, `createCellStyle()`, `write(OutputStream)` | `ExcelUtil` |
| `Sheet` | `createRow(int)`, `setColumnWidth(int, int)`, `createFreezePane(int, int)` | `ExcelUtil` |
| `Row` | `createCell(int)` | `ExcelUtil` |
| `Cell` | `setCellValue(String)`, `setCellStyle(CellStyle)` | `ExcelUtil` |
| `Font` | `setBold(boolean)`, `setFontHeightInPoints(short)`, `setColor(short)` | `ExcelUtil.createHeaderStyle()` |
| `CellStyle` | `setFont(Font)`, `setFillForegroundColor(short)`, `setFillPattern(FillPatternType)`, `setAlignment(HorizontalAlignment)`, `setVerticalAlignment(VerticalAlignment)`, `setBorderBottom(BorderStyle)`, `setBorderTop(BorderStyle)`, `setBorderLeft(BorderStyle)`, `setBorderRight(BorderStyle)`, `setWrapText(boolean)` | `ExcelUtil` |
| `IndexedColors` | `WHITE.getIndex()`, `DARK_BLUE.getIndex()` | `ExcelUtil.createHeaderStyle()` |
| `FillPatternType` | `SOLID_FOREGROUND` | `ExcelUtil.createHeaderStyle()` |
| `HorizontalAlignment` | `CENTER` | `ExcelUtil.createHeaderStyle()` |
| `VerticalAlignment` | `CENTER` | `ExcelUtil` |
| `BorderStyle` | `THIN` | `ExcelUtil` |

### 5.7 Jackson (com.fasterxml.jackson)

| 类/注解 | 作用 | 使用位置 |
|---------|------|---------|
| `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")` | 控制日期序列化格式 | `Customer.createdAt`, `Customer.updatedAt` |
| `ObjectMapper` | JSON ↔ Java 转换（由 Spring 自动调用） | 隐式使用 |

---

## 附录：学习方法建议

### 从"会用"到"理解"的路径

1. **先看本文档的 3.1 节（新增客户）**：它是调用链路最短的接口，理解每个注解和方法的作用；
2. **再看 3.5 节（分页查询）**：重点理解 `LambdaQueryWrapper` 的方法链式调用 + `Page` 分页拦截器；
3. **最后看 3.6 节（Excel导出）**：涉及 POI + Servlet 两大外部 API 体系；
4. **在 IDE 中 Ctrl+Click** 每个方法名，跳转到源码看它的完整实现；
5. **在 Service 层打断点**，Debug 一次完整的请求流程，观察每个变量的值。

### 可用于面试的知识点

- "Spring 如何将 JSON 请求体转为 Java 对象？" → 答：`@RequestBody` + `MappingJackson2HttpMessageConverter` + Jackson `ObjectMapper.readValue()`
- "MyBatis-Plus 的逻辑删除是怎么实现的？" → 答：`@TableLogic` + 内置拦截器把 DELETE 改写为 UPDATE，所有 SELECT 自动加 `is_deleted=0`
- "LambdaQueryWrapper 相比字符串字段名有什么优势？" → 答：编译期类型检查，方法引用 → 反射解析字段名，字段改名时编译器直接报错
- "分页查询的 COUNT 和 SELECT 是怎么执行的？" → 答：`PaginationInnerInterceptor` 先执行 COUNT 查询获取总数，再拼接 LIMIT 执行数据查询

---

> **文档状态**：已发布  
> **最后更新**：2026-06-30  
> **建议配合阅读**：《API详细讲解文档》《技术文档》《设计文档》
