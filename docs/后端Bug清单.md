# 后端 Bug 清单

> 审查范围：Controller / Service / Config / Entity / application.yml
> 日期：2026-07-05

---

## 🔴 致命（功能异常 / 请求失败）

### Bug 1 — update 接口用错 HTTP 方法注解 → 前端 PUT 请求 405

**文件**：`CustomerController.java:94`

```java
// ❌ 错误：用了 @PostMapping
@PostMapping("/{id}")
public Result<Customer> update(@PathVariable @NotNull Long id, @RequestBody Customer customer) {
```

前端发送 `PUT /api/customers/8`，但后端在 `/{id}` 路径上只注册了 GET、POST、DELETE，没有 PUT。

| 已注册的方法 | 注解 | 前端请求的方法 | 匹配？ |
|-------------|------|-------------|-------|
| GET | `@GetMapping("/{id}")` | — | — |
| DELETE | `@DeleteMapping("/{id}")` | — | — |
| **POST** | `@PostMapping("/{id}")` | — | — |
| 无 | — | **PUT** | ❌ 405 |

**改正**：

```java
@PutMapping("/{id}")   // ← POST 改成 PUT
public Result<Customer> update(...)
```

---

### Bug 2 — 全局异常处理器缺少 `@ControllerAdvice` → 所有异常处理失效

**文件**：`GlobalExceptionHandler.java:25`

```java
@Slf4j
public class GlobalExceptionHandler {  // ← 缺少 @RestControllerAdvice
```

Spring 不会把这个类识别为全局异常处理器。里面 5 个 `@ExceptionHandler` 方法全是死代码，永远不会执行。

**后果**：所有异常（参数校验失败、业务异常 RuntimeException、系统异常）都走 Spring 默认处理，返回的是 500 内部错误页面的 HTML，而不是 `Result` 格式的 JSON。前端收到的错误信息无法正确解析。

| 异常 | 预期返回 | 实际返回 |
|------|---------|---------|
| `@Valid` 校验失败 | `{"code":400, "message":"公司名称不能为空"}` | 500 HTML |
| `RuntimeException("客户不存在")` | `{"code":400, "message":"客户不存在"}` | 500 HTML |
| 其他未处理异常 | `{"code":500, "message":"服务器内部错误"}` | 500 HTML + 堆栈 |

**改正**：加上 `@RestControllerAdvice` 注解：

```java
@Slf4j
@RestControllerAdvice          // ← 加这个
public class GlobalExceptionHandler {
```

---

## 🟡 中等（功能正确但有副作用或无效配置）

### Bug 3 — `logging` 配置缩进在 `mybatis-plus` 下 → 日志级别不生效

**文件**：`application.yml:51`

```yaml
mybatis-plus:
  type-aliases-package: com.crm.entity
  configuration: ...
  global-config: ...
  
  logging:                    # ← 这里！和 type-aliases-package 同级（2空格缩进），
    level:                    #   意味着它是 mybatis-plus 的子属性
      com.crm: debug
      org.springframework: info
```

YAML 缩进规则：`logging:` 前面 2 个空格，和 `type-aliases-package` 同级，属于 `mybatis-plus` 的下级属性。但 `logging` 是 Spring Boot 的根级别配置，MyBatis-Plus 不认识它，直接忽略。

**后果**：`com.crm` 包的 DEBUG 日志、`org.springframework` 的 INFO 日志都不生效，相当于没配日志级别。

**改正**：把 `logging:` 提到根级别（0 空格缩进），和 `spring:`、`mybatis-plus:` 平级：

```yaml
mybatis-plus:
  ...
  
logging:                       # ← 根级别（顶格）冒号后面加一个空格
  level:
    com.crm: debug
    org.springframework: info
```

---

### Bug 4 — `PaginationInnerInterceptor.setMaxLimit(500)` 限制了 Excel 导出

**文件**：`MyBatisPlusConfig.java:22` + `CustomerServiceImpl.java:101`

```java
// MyBatisPlusConfig.java
pagination.setMaxLimit(500L);  // ← 单页最大 500 条

// CustomerServiceImpl.java exportExcel()
queryDTO.setPageSize(Integer.MAX_VALUE);  // ← 想导出全部，但被 500 截断
customerMapper.selectList(wrapper);  // 实际上这里不走分页插件，影响不大
```

实际上 `exportExcel` 最终调用的是 `selectList`（不是 `selectPage`），分页拦截器不拦截 `selectList`，所以当前代码不受影响。但如果未来有分页查询场景需要一次查超过 500 条，会被截断。

**建议**：增加 `maxLimit` 或在导出时不走分页逻辑，当前可以先不改。

---

### Bug 5 — `main` 配置文件写入了测试用的 `sql.init.mode: always`

**文件**：`application.yml:8-9`

```yaml
spring:
  sql:
    init:
      mode: always     # ← 每次启动都执行 schema.sql / data.sql
```

这个配置本应只放在 `test/resources/application.yml` 里。放在 `main` 下会导致生产环境每次启动都检查并执行初始化 SQL。虽然目前 `main/resources/` 下没有 `schema.sql`，暂无破坏性影响，但属于配置污染。

**改正**：把这 3 行移到 `test/resources/application.yml` 的 `spring:` 下面，从 `main` 删掉。

---

## 🟢 轻微（不影响功能，但不够规范）

### Bug 6 — delete 方法异常信息格式不一致

**文件**：`CustomerServiceImpl.java:66`

```java
throw new RuntimeException("客户不存在: id" + id);   // 输出：客户不存在: id8
```

`update` 方法里是 `"客户不存在：id=" + id`，delete 里漏了 `=`。导致错误信息变成 `客户不存在: id8` 而不是 `客户不存在: id=8`。

**改正**：

```java
throw new RuntimeException("客户不存在: id=" + id);
```

---

### Bug 7 — Entity 注解 `@TableField(fill = FieldFill.INSERT_UPDATE)` 缺少自动填充处理器

**文件**：`Customer.java:57,61`

```java
@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime createdAt;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updatedAt;
```

声明了自动填充策略，但没有实现 `MetaObjectHandler` 写填充逻辑。从日志可以看到插入时 `created_at` 和 `updated_at` 的值是 `null`：

```
==> Parameters: ..., null, null
```

除非数据库表设置了 `DEFAULT CURRENT_TIMESTAMP`，否则这两个时间字段会是 NULL。

**改正**：新增一个 `MetaObjectHandler` 实现类：

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

---

## 总结

| # | 严重度 | 位置 | 问题 | 现象 |
|---|--------|------|------|------|
| 1 | 🔴 致命 | `CustomerController.java:94` | `@PostMapping` 应为 `@PutMapping` | 编辑保存时 405 |
| 2 | 🔴 致命 | `GlobalExceptionHandler.java:25` | 缺少 `@RestControllerAdvice` | 异常返回 HTML 而非 JSON |
| 3 | 🟡 中等 | `application.yml:51` | `logging` 缩进在 `mybatis-plus` 下 | 日志级别配置不生效 |
| 4 | 🟡 中等 | `MyBatisPlusConfig.java:22` | `setMaxLimit(500)` 限制查询条数 | 暂不影响，未来有隐患 |
| 5 | 🟡 中等 | `application.yml:8` | `sql.init.mode: always` 应只放测试配置 | 生产环境启动行为异常 |
| 6 | 🟢 轻微 | `CustomerServiceImpl.java:66` | 异常信息缺 `=` | 日志可读性差 |
| 7 | 🟢 轻微 | `Customer.java:57,61` | `@TableField(fill=...)` 缺少 `MetaObjectHandler` | 时间字段为 null |
